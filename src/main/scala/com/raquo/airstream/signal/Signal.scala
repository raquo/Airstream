package com.raquo.airstream.signal

import com.raquo.airstream.core.{Observable, Observer, Subscription, Transaction}
import com.raquo.airstream.eventstream.{EventStream, MapEventStream}
import com.raquo.airstream.features.{CombineObservable, FlattenStrategy}
import com.raquo.airstream.ownership.Owner

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** Signal is an Observable with a current value. */
trait Signal[+A] extends Observable[A] {

  override type Self[+T] = Signal[T]

  /** Evaluate initial value of this [[Signal]].
    * This method must only be called once, when this value is first needed.
    * You should override this method as `def` (no `val` or lazy val) to avoid
    * holding a reference to the initial value beyond the duration of its relevance.
    */
  // @TODO[Integrity] ^^^ Does this memory management advice even hold water?
  protected[this] def initialValue: Try[A]

  protected[this] var maybeLastSeenCurrentValue: js.UndefOr[Try[A]] = js.undefined

  /** @param project Note: guarded against exceptions */
  override def map[B](project: A => B): Signal[B] = {
    new MapSignal(parent = this, project, recover = None)
  }

  /** @param operator Note: Must not throw! */
  def compose[B](operator: Signal[A] => Signal[B]): Signal[B] = {
    operator(this)
  }

  def combineWith[AA >: A, B](otherSignal: Signal[B]): CombineSignal2[AA, B, (AA, B)] = {
    new CombineSignal2(
      parent1 = this,
      parent2 = otherSignal,
      combinator = CombineObservable.guardedCombinator((_, _))
    )
  }

  def changes: EventStream[A] = new MapEventStream[A, A](parent = this, project = identity, recover = None)

  /**
    * @param makeInitial Note: guarded against exceptions
    * @param fn Note: guarded against exceptions
    */
  def fold[B](makeInitial: A => B)(fn: (B, A) => B): Signal[B] = {
    foldRecover(
      parentInitial => parentInitial.map(makeInitial)
    )(
      (currentValue, nextParentValue) => Try(fn(currentValue.get, nextParentValue.get))
    )
  }

  // @TODO[Naming]
  /**
    * @param makeInitial currentParentValue => initialValue
    * @param fn (currentValue, nextParentValue) => nextValue
    * @return
    */
  def foldRecover[B](makeInitial: Try[A] => Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    new FoldSignal(
      parent = this,
      makeInitialValue = () => makeInitial(tryNow()),
      fn
    )
  }

  /** @param pf Note: guarded against exceptions */
  override def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Signal[B] = {
    new MapSignal[A, B](
      this,
      project = identity,
      recover = Some(pf)
    )
  }

  override def recoverToTry: Signal[Try[A]] = map(Try(_)).recover[Try[A]] { case err => Some(Failure(err)) }

  /** Add a noop observer to this signal to ensure that it's started.
    * This lets you access .now and .tryNow on the resulting SignalViewer.
    *
    * You can use `myStream.toWeakSignal.observe` to read the last emitted
    * value from event streams just as well.
    */
  def observe(implicit owner: Owner): SignalViewer[A] = new SignalViewer(this, owner)

  /** Initial value is only evaluated if/when needed (when there are observers) */
  protected[airstream] def tryNow(): Try[A] = {
    maybeLastSeenCurrentValue.getOrElse {
      val currentValue = initialValue
      setCurrentValue(currentValue)
      currentValue
    }
  }

  /** See comment for [[tryNow]] right above
    *
    * @throws Exception if current value is an error
    */
  protected[airstream] def now(): A = tryNow().get

  protected[this] def setCurrentValue(newValue: Try[A]): Unit = {
    maybeLastSeenCurrentValue = js.defined(newValue)
  }

  /** Here we need to ensure that Signal's default value has been evaluated.
    * It is important because if a Signal gets started by means of its .changes
    * stream acquiring an observer, nothing else would trigger this evaluation
    * because initialValue is not directly used by the .changes stream.
    * However, when the events start coming in, we will need this initialValue
    * because Signal needs to know when its current value has changed.
    */
  override protected[this] def onStart(): Unit = {
    tryNow() // trigger setCurrentValue if we didn't initialize this before
    super.onStart()
  }

  // @TODO[API] Use pattern match instead when isInstanceOf performance is fixed: https://github.com/scala-js/scala-js/issues/2066
  override protected def onAddedExternalObserver(observer: Observer[A]): Unit = {
    super.onAddedExternalObserver(observer)
    observer.onTry(tryNow()) // send current value immediately
  }

  override protected[this] final def fireValue(nextValue: A, transaction: Transaction): Unit = {
    fireTry(Success(nextValue), transaction)
  }

  override protected[this] final def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    fireTry(Failure(nextError), transaction)
  }

  /** Signal propagates only if its value has changed */
  override protected[this] def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    // @TODO[API] It is rather curious/unintuitive that firing external observers first seems to make more sense. Think about it some more.
    // @TODO[Performance] This might be suboptimal for some data structures (e.g. big maps). Document this along with workarounds.
    if (tryNow() != nextValue) {
      setCurrentValue(nextValue)

      // === CAUTION ===
      // The following logic must match EventStream's fireValue / fireError! It is separated here for performance.

      val isError = nextValue.isFailure
      var errorReported = false

      var index = 0
      while (index < externalObservers.length) {
        externalObservers(index).onTry(nextValue)
        index += 1
      }
      if (index > 0 && isError) errorReported = true

      index = 0
      while (index < internalObservers.length) {
        internalObservers(index).onTry(nextValue, transaction)
        index += 1
      }
      if (index > 0 && isError) errorReported = true
    }
  }
}

object Signal {

  @inline def fromFuture[A](future: Future[A]): Signal[Option[A]] = {
    new FutureSignal(future)
  }

  implicit def toTuple2Signal[A, B](signal: Signal[(A, B)]): Tuple2Signal[A, B] = {
    new Tuple2Signal(signal)
  }
}
