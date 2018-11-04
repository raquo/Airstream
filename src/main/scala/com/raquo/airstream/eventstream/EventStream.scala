package com.raquo.airstream.eventstream

import com.raquo.airstream.core.AirstreamError.ObserverError
import com.raquo.airstream.core.{AirstreamError, LazyObservable, MemoryObservable, Transaction}
import com.raquo.airstream.features.CombineObservable
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.signal.{FoldSignal, Signal, SignalFromEventStream, SignalViewer}
import com.raquo.airstream.state.{MapState, State}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait EventStream[+A] extends LazyObservable[A] {

  override type Self[+T] = EventStream[T]

  override def map[B](project: A => B): EventStream[B] = {
    new MapEventStream(this, project, recover = None)
  }

  /** `value` is passed by name, so it will be evaluated on every event.
    * Use it to sample mutable values (e.g. myInput.ref.value in Laminar).
    *
    * See also: [[mapToValue]]
    *
    * @param value Note: guarded against exceptions
    */
  def mapTo[B](value: => B): EventStream[B] = {
    new MapEventStream[A, B](parent = this, project = _ => value, recover = None)
  }

  /** `value` is evaluated only once, when this method is called.
    *
    * See also: [[mapTo]]
    */
  def mapToValue[B](value: B): EventStream[B] = {
    new MapEventStream[A, B](parent = this, project = _ => value, recover = None)
  }

  /** @param passes Note: guarded against exceptions */
  def filter(passes: A => Boolean): EventStream[A] = {
    new FilterEventStream(parent = this, passes)
  }

  def filterNot(predicate: A => Boolean): EventStream[A] = filter(!predicate(_))

  /** @param pf Note: guarded against exceptions */
  def collect[B](pf: PartialFunction[A, B]): EventStream[B] = {
    // @TODO[Performance] Use applyOrElse
    filter(pf.isDefinedAt).map(pf)
  }

  def delay(intervalMillis: Int = 0): EventStream[A] = {
    new DelayEventStream(parent = this, intervalMillis)
  }

  /** See docs for [[ThrottleEventStream]] */
  def throttle(intervalMillis: Int): EventStream[A] = {
    ThrottleEventStream(parent = this, intervalMillis)
  }

  /** See docs for [[DebounceEventStream]] */
  def debounce(delayFromLastEventMillis: Int): EventStream[A] = {
    new DebounceEventStream(parent = this, delayFromLastEventMillis)
  }

  // @TODO[API] Should we introduce some kind of FoldError() wrapper?
  /** @param fn Note: guarded against exceptions */
  def fold[B](initial: B)(fn: (B, A) => B): Signal[B] = {
    foldRecover(
      Success(initial)
    )(
      (currentValue, nextParentValue) => Try(fn(currentValue.get, nextParentValue.get))
    )
  }

  // @TODO Naming
  /** @param fn Note: Must not throw! */
  def foldRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    new FoldSignal(parent = this, () => initial, fn)
  }

  def toSignal[B >: A](initial: B): Signal[B] = {
    toSignalWithTry(Success(initial))
  }

  def toSignalWithTry[B >: A](initial: Try[B]): Signal[B] = {
    new SignalFromEventStream(parent = this, initial)
  }

  def toWeakSignal: Signal[Option[A]] = {
    new SignalFromEventStream(parent = this.map(Some(_)), initialValue = Success(None))
  }

  def toState[B >: A](initial: B)(implicit owner: Owner): State[B] = {
    toStateWithTry(Success(initial))(owner)
  }

  def toStateWithTry[B >: A](initial: Try[B])(implicit owner: Owner): State[B] = {
    new MapState[B, B](parent = this.toSignalWithTry(initial), project = identity, recover = None, owner)
  }

  def compose[B](operator: EventStream[A] => EventStream[B]): EventStream[B] = {
    operator(this)
  }

  def combineWith[AA >: A, B](otherEventStream: EventStream[B]): CombineEventStream2[AA, B, (AA, B)] = {
    new CombineEventStream2(
      parent1 = this,
      parent2 = otherEventStream,
      combinator = CombineObservable.guardedCombinator((_, _))
    )
  }

  def withCurrentValueOf[B](memoryObservable: MemoryObservable[B]): EventStream[(A, B)] = {
    new SampleCombineEventStream2[A, B, (A, B)](
      samplingStream = this,
      sampledMemoryObservable = memoryObservable,
      combinator = CombineObservable.guardedCombinator((_, _))
    )
  }

  def sample[B](memoryObservable: MemoryObservable[B]): EventStream[B] = {
    new SampleCombineEventStream2[A, B, B](
      samplingStream = this,
      sampledMemoryObservable = memoryObservable,
      combinator = CombineObservable.guardedCombinator((_, sampledValue) => sampledValue)
    )
  }

  /** See docs for [[MapEventStream]]
    *
    * @param pf Note: guarded against exceptions
    */
  override def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Self[B] = {
    new MapEventStream[A, B](
      parent = this,
      project = identity,
      recover = Some(pf)
    )
  }

  override def recoverToTry: EventStream[Try[A]] = map(Try(_)).recover[Try[A]] { case err => Some(Failure(err)) }

  override protected[this] def fireValue(nextValue: A, transaction: Transaction): Unit = {
    // Note: Removal of observers is always done at the end of a transaction, so the iteration here is safe

    // === CAUTION ===
    // The following logic must match MemoryObservable's fireTry! It is separated here for performance.

    var index = 0
    while (index < externalObservers.length) {
      try {
        externalObservers(index).onNext(nextValue)
      } catch {
        case err: Throwable => AirstreamError.sendUnhandledError(ObserverError(err))
      }
      index += 1
    }

    index = 0
    while (index < internalObservers.length) {
      internalObservers(index).onNext(nextValue, transaction)
      index += 1
    }
  }

  override protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    // Note: Removal of observers is always done at the end of a transaction, so the iteration here is safe

    // === CAUTION ===
    // The following logic must match MemoryObservable's fireTry! It is separated here for performance.

    var index = 0
    while (index < externalObservers.length) {
      externalObservers(index).onError(nextError)
      index += 1
    }

    index = 0
    while (index < internalObservers.length) {
      internalObservers(index).onError(nextError, transaction)
      index += 1
    }
  }

  override protected[this] final def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    nextValue.fold(
      fireError(_, transaction),
      fireValue(_, transaction)
    )
  }
}

object EventStream {

  @deprecated("Use fromValue (see docs)", "0.4")
  def fromSeq[A](events: Seq[A]): EventStream[A] = {
    new SeqEventStream[A](events.map(Success(_)))
  }

  def fromValue[A](event: A): EventStream[A] = {
    new SeqEventStream[A](List(Success(event)))
  }

  // @TODO[API] Do we want to keep this easily exposed?
  def fromTry[A](value: Try[A]): EventStream[A] = {
    new SeqEventStream[A](List(value))
  }

  def fromFuture[A](future: Future[A]): EventStream[A] = {
    new FutureEventStream(future, emitIfFutureCompleted = false)
  }

  @inline def combine[A, B](
    stream1: EventStream[A],
    stream2: EventStream[B]
  ): EventStream[(A, B)] = {
    stream1.combineWith(stream2)
  }

  def merge[A](streams: EventStream[A]*): EventStream[A] = {
    new MergeEventStream[A](streams)
  }

  implicit def toTuple2Stream[A, B](stream: EventStream[(A, B)]): Tuple2EventStream[A, B] = {
    new Tuple2EventStream(stream)
  }
}
