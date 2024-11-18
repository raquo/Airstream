package com.raquo.airstream.core

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

trait WritableSignal[A] extends Signal[A] with WritableObservable[A] {

  protected var maybeLastSeenCurrentValue: js.UndefOr[Try[A]] = js.undefined

  protected def setCurrentValue(newValue: Try[A]): Unit = {
    val isInitial = maybeLastSeenCurrentValue.isEmpty
    // println(s"${this} > setCurrentValue > ${newValue}, isInitial = ${isInitial}")
    if (!isInitial) {
      _lastUpdateId = Signal.nextUpdateId()
    }
    maybeLastSeenCurrentValue = newValue
  }

  /** Note: Initial value is only evaluated if/when needed (when there are observers) */
  override protected[airstream] def tryNow(): Try[A] = {
    // dom.console.log(s"${this} > tryNow (maybeLastSeenCurrentValue = ${maybeLastSeenCurrentValue})")
    maybeLastSeenCurrentValue.getOrElse {
      // #TODO[Integrity] I'm not sure if updating `_lastUpdateId` here is right.
      //  - I expected `0` to indicate "initial value" (no events emitted yet),
      //    as mentioned in Signal.nextUpdateId, but it seems that it's only `0`
      //    until the signal's initial value is evaluated, then we increment it here.
      //  - I'm not sure if this is done on purpose or not, it's possible that the
      //    comment is incorrect. Either way, need to figure this out some time.
      //  - Currently, tests pass either way, but it's hard to tell definitely
      //    if this change would really be ok.
      // dom.console.log(s"${this} -> eval current value from tryNow")
      _lastUpdateId = Signal.nextUpdateId()
      val nextValue = currentValueFromParent()
      setCurrentValue(nextValue)
      nextValue
    }
  }

  final override protected def fireValue(nextValue: A, transaction: Transaction): Unit = {
    fireTry(Success(nextValue), transaction)
  }

  final override protected def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    fireTry(Failure(nextError), transaction)
  }

  override protected def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    // println(s"$this > FIRE > $nextValue")

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
      pendingObserverRemovals.forEach(remove => remove())
      pendingObserverRemovals.length = 0
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
