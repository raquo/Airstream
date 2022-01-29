package com.raquo.airstream.core

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

trait WritableSignal[A] extends Signal[A] with WritableObservable[A] {

  protected var maybeLastSeenCurrentValue: js.UndefOr[Try[A]] = js.undefined

  protected def setCurrentValue(newValue: Try[A]): Unit = {
    maybeLastSeenCurrentValue = js.defined(newValue)
  }

  /** Note: Initial value is only evaluated if/when needed (when there are observers) */
  override protected[airstream] def tryNow(): Try[A] = {
    maybeLastSeenCurrentValue.getOrElse {
      val nextValue = currentValueFromParent()
      setCurrentValue(nextValue)
      nextValue
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
    //println(s"$this > FIRE > $nextValue")

    setCurrentValue(nextValue)

    // === CAUTION ===
    // The following logic must match EventStream's fireValue / fireError! It is separated here for performance.

    val isError = nextValue.isFailure
    var errorReported = false

    // @TODO[API] It is rather curious/unintuitive that firing external observers first seems to make more sense. Think about it some more.

    isSafeToRemoveObserver = false

    externalObservers.foreach { observer =>
      observer.onTry(nextValue)
      if (isError && !errorReported) errorReported = true
    }

    internalObservers.foreach { observer =>
      InternalObserver.onTry(observer, nextValue, transaction)
      if (isError && !errorReported) errorReported = true
    }

    isSafeToRemoveObserver = true

    maybePendingObserverRemovals.foreach { pendingObserverRemovals =>
      pendingObserverRemovals.foreach(remove => remove())
      pendingObserverRemovals.clear()
    }

    // This will only ever happen for special Signals that maintain their current value even without observers.
    // Currently we only have one kind of such signal: StrictSignal.
    //
    // We want to report unhandled errors on such signals if they have no observers (including internal observers)
    // because if we don't, the error will not be reported anywhere, and I think we would usually want it to be reported.
    if (isError && !errorReported) {
      nextValue.fold(AirstreamError.sendUnhandledError, _ => ())
    }
  }

}
