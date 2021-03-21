package com.raquo.airstream.core

import scala.scalajs.js
import scala.util.{ Failure, Success, Try }

trait WritableSignal[A] extends WritableObservable[A] with SignalOps[A] {

  /** Evaluate initial value of this [[Signal]].
    * This method must only be called once, when this value is first needed.
    * You should override this method as `def` (no `val` or lazy val) to avoid
    * holding a reference to the initial value beyond the duration of its relevance.
    */
  // @TODO[Integrity] ^^^ Does this memory management advice even hold water?
  protected def initialValue: Try[A]

  protected var maybeLastSeenCurrentValue: js.UndefOr[Try[A]] = js.undefined

  /** See comment for [[tryNow]] right above
    *
    * @throws Exception if current value is an error
    */
  protected[airstream] def now(): A = tryNow().get

  protected def setCurrentValue(newValue: Try[A]): Unit = {
    maybeLastSeenCurrentValue = js.defined(newValue)
  }

  /** Initial value is only evaluated if/when needed (when there are observers) */
  protected[airstream] def tryNow(): Try[A] = {
    maybeLastSeenCurrentValue.getOrElse {
      val currentValue = initialValue
      setCurrentValue(currentValue)
      currentValue
    }
  }

  override protected final def fireValue(nextValue: A, transaction: Transaction): Unit = {
    fireTry(Success(nextValue), transaction)
  }

  override protected final def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    fireTry(Failure(nextError), transaction)
  }

  /** Signal propagates only if its value has changed */
  override protected def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    // @TODO[API] It is rather curious/unintuitive that firing external observers first seems to make more sense. Think about it some more.
    // @TODO[Performance] This might be suboptimal for some data structures (e.g. big maps). Document this along with workarounds.
    // Note: This comparison is using the Scala `equals` method. Typically `equals` calls `eq` first,
    // to check for reference equality, then proceeds to check for structural equality if needed.
    if (tryNow() != nextValue) {
      setCurrentValue(nextValue)

      // === CAUTION ===
      // The following logic must match EventStream's fireValue / fireError! It is separated here for performance.

      val isError = nextValue.isFailure
      var errorReported = false

      ObserverRegistry.getExternalObservers(this).foreach { observer =>
        observer.onTry(nextValue)
        if (isError && !errorReported) errorReported = true
      }

      ObserverRegistry.getInternalObservers(this).foreach { observer =>
        observer.onTry(nextValue, transaction)
        if (isError && !errorReported) errorReported = true
      }

      // This will only ever happen for special Signals that maintain their current value even without observers.
      // Currently we only have one kind of such signal: StrictSignal.
      //
      // We want to report unhandled errors on such signals if they have no observers (including internal observers)
      // because if we don't, the error will not be reported anywhere, and I think we would usually want it.
      if (isError && !errorReported) {
        nextValue.fold(AirstreamError.sendUnhandledError, _ => ())
      }
    }
  }

}
