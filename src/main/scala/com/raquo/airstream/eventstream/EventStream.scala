package com.raquo.airstream.eventstream

import com.raquo.airstream.core.AirstreamError.ObserverError
import com.raquo.airstream.core.{AirstreamError, Observable, Observer, Transaction}
import com.raquo.airstream.custom.CustomSource._
import com.raquo.airstream.custom.{CustomSource, CustomStreamSource}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.features.CombineObservable
import com.raquo.airstream.signal.{FoldLeftSignal, Signal, SignalFromEventStream}

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

trait EventStream[+A] extends Observable[A] {

  override type Self[+T] = EventStream[T]

  override def map[B](project: A => B): EventStream[B] = {
    new MapEventStream(this, project, recover = None)
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

  /** @param ms milliseconds of delay */
  def delay(ms: Int = 0): EventStream[A] = {
    new DelayEventStream(parent = this, ms)
  }

  /** Make a stream that emits this stream's values but waits for `after` stream to emit first in a given transaction.
    * You can use this for Signals too with `Signal.composeChanges` (see docs for more details)
    */
  def delaySync(after: EventStream[_]): EventStream[A] = {
    new SyncDelayEventStream[A](parent = this, after = after)
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
  def foldLeft[B](initial: B)(fn: (B, A) => B): Signal[B] = {
    foldLeftRecover(
      Success(initial)
    )(
      (currentValue, nextParentValue) => Try(fn(currentValue.get, nextParentValue.get))
    )
  }

  /** @param fn Note: Must not throw! */
  def foldLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    new FoldLeftSignal(parent = this, () => initial, fn)
  }

  @inline def startWith[B >: A](initial: => B): Signal[B] = toSignal(initial)

  @inline def startWithTry[B >: A](initial: => Try[B]): Signal[B] = toSignalWithTry(initial)

  @inline def startWithNone: Signal[Option[A]] = toWeakSignal

  override def toSignal[B >: A](initial: => B): Signal[B] = {
    toSignalWithTry(Success(initial))
  }

  def toSignalWithTry[B >: A](initial: => Try[B]): Signal[B] = {
    new SignalFromEventStream(parent = this, initial)
  }

  def toWeakSignal: Signal[Option[A]] = {
    new SignalFromEventStream(parent = this.map(Some(_)), lazyInitialValue = Success(None))
  }

  def compose[B](operator: EventStream[A] => EventStream[B]): EventStream[B] = {
    operator(this)
  }

  def combineWith[AA >: A, B](otherEventStream: EventStream[B]): EventStream[(AA, B)] = {
    new CombineEventStream2[AA, B, (AA, B)](
      parent1 = this,
      parent2 = otherEventStream,
      combinator = CombineObservable.guardedCombinator((_, _))
    )
  }

  def withCurrentValueOf[B](signal: Signal[B]): EventStream[(A, B)] = {
    new SampleCombineEventStream2[A, B, (A, B)](
      samplingStream = this,
      sampledSignal = signal,
      combinator = CombineObservable.guardedCombinator((_, _))
    )
  }

  def sample[B](signal: Signal[B]): EventStream[B] = {
    new SampleCombineEventStream2[A, B, B](
      samplingStream = this,
      sampledSignal = signal,
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
    // The following logic must match Signal's fireTry! It is separated here for performance.

    externalObservers.foreach { observer =>
      try {
        observer.onNext(nextValue)
      } catch {
        case err: Throwable => AirstreamError.sendUnhandledError(ObserverError(err))
      }
    }

    internalObservers.foreach { observer =>
      observer.onNext(nextValue, transaction)
    }
  }

  override protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    // Note: Removal of observers is always done at the end of a transaction, so the iteration here is safe

    // === CAUTION ===
    // The following logic must match Signal's fireTry! It is separated here for performance.

    externalObservers.foreach { observer =>
      observer.onError(nextError)
    }

    internalObservers.foreach { observer =>
      observer.onError(nextError, transaction)
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

  /** Event stream that never emits anything */
  val empty: EventStream[Nothing] = {
    fromCustomSource[Nothing](
      shouldStart = _ => false,
      start = (_, _, _, _) => (),
      stop = _ => ()
    )
  }

  /** @param emitOnce if true, the event will be emitted at most one time.
    *                 If false, the event will be emitted every time the stream is started. */
  def fromSeq[A](events: Seq[A], emitOnce: Boolean): EventStream[A] = {
    fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, _, _, _) => events.foreach(fireEvent),
      stop = _ => ()
    )
  }

  /** @param emitOnce if true, the event will be emitted at most one time.
    *                 If false, the event will be emitted every time the stream is started. */
  def fromValue[A](event: A, emitOnce: Boolean): EventStream[A] = {
    fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, _, _, _) => fireEvent(event),
      stop = _ => ()
    )
  }

  /** @param emitOnce if true, the event will be emitted at most one time.
    *                 If false, the event will be emitted every time the stream is started. */
  def fromTry[A](value: Try[A], emitOnce: Boolean): EventStream[A] = {
    fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, fireError, _, _) => value.fold(fireError, fireEvent),
      stop = _ => ()
    )
  }

  def fromFuture[A](future: Future[A], emitFutureIfCompleted: Boolean = false): EventStream[A] = {
    new FutureEventStream[A](future, emitFutureIfCompleted)
  }

  @inline def fromJsPromise[A](promise: js.Promise[A]): EventStream[A] = {
    fromFuture(promise.toFuture)
  }

  /** Easy helper for custom events. See [[CustomStreamSource]] for docs.
    *
    * @param stop MUST NOT THROW!
    */
  def fromCustomSource[A](
    shouldStart: StartIndex => Boolean = _ => true,
    start: (FireValue[A], FireError, GetStartIndex, GetIsStarted) => Unit,
    stop: StartIndex => Unit
  ): EventStream[A] = {
    CustomStreamSource[A] { (fireValue, fireError, getStartIndex, getIsStarted) =>
      CustomSource.Config(
        onStart = {
          start(fireValue, fireError, getStartIndex, getIsStarted)
        },
        onStop = {
          stop(getStartIndex())
        }
      ).when {
        () => shouldStart(getStartIndex())
      }
    }
  }

  /** Create a stream and a callback that, when fired, makes that stream emit. */
  def withCallback[A]: (EventStream[A], A => Unit) = {
    val bus = new EventBus[A]
    (bus.events, bus.writer.onNext)
  }

  /** Create a stream and a JS callback that, when fired, makes that stream emit. */
  def withJsCallback[A]: (EventStream[A], js.Function1[A, Unit]) = {
    val bus = new EventBus[A]
    (bus.events, bus.writer.onNext)
  }

  /** Create a stream and an observer that, when receiving an event or an error, makes that stream emit. */
  def withObserver[A]: (EventStream[A], Observer[A]) = {
    val bus = new EventBus[A]
    (bus.events, bus.writer)
  }

  def periodic(
    intervalMs: Int,
    emitInitial: Boolean = true,
    resetOnStop: Boolean = true
  ): PeriodicEventStream[Int] = {
    new PeriodicEventStream[Int](
      initial = 0,
      next = eventNumber => Some((eventNumber + 1, intervalMs)),
      emitInitial = emitInitial,
      resetOnStop = resetOnStop
    )
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

  implicit def toSplittableStream[M[_], Input](stream: EventStream[M[Input]]): SplittableEventStream[M, Input] = {
    new SplittableEventStream(stream)
  }

  implicit def toSplittableOneStream[A](stream: EventStream[A]): SplittableOneEventStream[A] = {
    new SplittableOneEventStream(stream)
  }
}
