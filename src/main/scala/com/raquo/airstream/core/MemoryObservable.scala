package com.raquo.airstream.core

import com.raquo.airstream.eventstream.{EventStream, MapEventStream}
import com.raquo.airstream.ownership.Owner

import scala.util.{Failure, Success, Try}

/** An observable that remembers its current value */
trait MemoryObservable[+A] extends Observable[A] {

  /** We don't want public access to this for lazy MemoryObservables (Signals) because if you called .now() on a Signal,
    * you wouldn't know if you were getting the updated value or not, because that would depend on whether said Signal
    * has observers.
    *
    * Use EventStream.sample and EventStream.withCurrentValueOf if you need this on a Signal.
    */
  protected[airstream] def tryNow(): Try[A]

  /** See comment for [[tryNow]] right above
    *
    * @throws Exception if current value is an error
    */
  protected[airstream] def now(): A = tryNow().get

  /** Evaluate initial value of this [[MemoryObservable]].
    * This method must only be called once, when this value is first needed.
    * You should override this method as `def` (no `val` or lazy val) to avoid
    * holding a reference to the initial value beyond the duration of its relevance.
    */
  protected[this] def initialValue: Try[A]

  /** Update the current value of this [[MemoryObservable]] */
  protected[this] def setCurrentValue(newValue: Try[A]): Unit

  def changes: EventStream[A] = new MapEventStream[A, A](parent = this, project = identity, recover = None)

  /** Note: if you want your observer to only get changes, subscribe to .changes stream instead */
  override def addObserver(observer: Observer[A])(implicit owner: Owner): Subscription = {
    val subscription = super.addObserver(observer)
    observer.onTry(tryNow()) // send current value immediately
    subscription
  }

  override protected[this] final def fireValue(nextValue: A, transaction: Transaction): Unit = {
    fireTry(Success(nextValue), transaction)
  }

  override protected[this] final def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    fireTry(Failure(nextError), transaction)
  }

  // @TODO[API] It is rather curious/unintuitive that firing external observers first seems to make more sense. Think about it some more.
  /** MemoryObservable propagates only if its value has changed */
  override protected[this] def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
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

      if (isError && !errorReported) {
        // This will only ever happen for State, because it is the only Observable that can run without observers
        // Basically, we want to report unhandled errors on State if it has no observers (including internal observers)
        // because if we don't, the error will not be reported anywhere.
        nextValue.fold(AirstreamError.sendUnhandledError, identity)
      }
    }
  }
}
