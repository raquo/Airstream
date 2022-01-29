package com.raquo.airstream.flatten

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.{InternalObserver, Protected, Signal, Transaction}

import scala.scalajs.js
import scala.util.{Success, Try}

/** This flattens a Signal[ Signal[A] ] into a Signal[A]
  *
  * When this signal is started, its current value tracks the current value of the last signal emitted by `parent`.
  *
  * This signal follows standard signal mechanics:
  * - It adds an internal observer to the signal that it's currently tracking while it's tracking it.
  * - It does not update when it is stopped, even if the signal being tracked is not stopped (e.g. if it has other observers).
  * - So if you want a consistent value out of this signal, keep it observed.
  */
class SwitchSignal[A](
  override protected[this] val parent: Signal[Signal[A]]
) extends SingleParentSignal[Signal[A], A] with InternalTryObserver[Signal[A]] {

  override protected val topoRank: Int = 1

  private[this] var maybeCurrentSignalTry: js.UndefOr[Try[Signal[A]]] = js.undefined

  private[this] def currentSignalTry: Try[Signal[A]] = maybeCurrentSignalTry.getOrElse {
    val initialSignal = parent.tryNow()
    maybeCurrentSignalTry = initialSignal
    initialSignal
  }

  private[this] val internalEventObserver: InternalObserver[A] = InternalObserver.fromTry[A](
    onTry = (nextTry, _) => {
      //println(s"> init trx from SwitchSignal.onValue($nextTry)")
      new Transaction(fireTry(nextTry, _))
    }
  )

  // #Note this is technically correct, I think, but in practice this method is not used in this Signal.
  override protected def currentValueFromParent(): Try[A] = {
    parent.tryNow().flatMap(_.tryNow())
  }

  override protected def updateCurrentValueFromParent(): Try[A] = {
    // #Note this is overriding the parent implementation
    switchToSignal(nextSignalTry = parent.tryNow(), isStarting = true)
    tryNow()
  }

  override protected def onTry(nextSignalTry: Try[Signal[A]], transaction: Transaction): Unit = {
    switchToSignal(nextSignalTry, isStarting = false)
  }

  private def switchToSignal(
    nextSignalTry: Try[Signal[A]],
    isStarting: Boolean
  ): Unit = {
    val isSameSignal = (nextSignalTry, currentSignalTry) match {
      case (Success(nextSignal), Success(currentSignal)) => nextSignal == currentSignal
      case _ => false
    }

    // If this SwitchSignal is starting, we just want to update its new current value,
    // we don't want to emit anything in a new transaction (if we did that instead, any
    // signals downstream of this signal would restart with a stale current value, and
    // then emit the updated value in that new transaction – that would be annoying,
    // I think)

    if (isSameSignal) {
      if (isStarting) {
        setCurrentValue(nextSignalTry.flatMap(_.tryNow()))

      } else {
        // We want to re-emit the signal's current value even if switching to the same signal to be consistent
        // with signals not having a built-in `==` check (since 0.15.0)
        //println(s"> init trx from SwitchSignal.onTry (same signal)")
        new Transaction(fireTry(nextSignalTry.flatMap(_.tryNow()), _)) // #Note[onStart,trx,loop]
      }

    } else {
      removeInternalObserverFromCurrentSignal()
      maybeCurrentSignalTry = nextSignalTry

      if (isStarting) {

        nextSignalTry.foreach(Protected.maybeWillStart)

        setCurrentValue(nextSignalTry.flatMap(_.tryNow()))

        nextSignalTry.foreach(_.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false))

      } else {

        //println(s"> init trx from SwitchSignal.onTry (new signal)")
        // Update this signal's value with nextSignal's current value (or an error if we don't have nextSignal)
        new Transaction(trx => {

          // #Note: Timing is important here.
          // 1. Create the `trx` transaction, since we need that boundary when flattening
          // 2. Ensure next signal is started by adding an internal observer to it
          // 3. Starting the signal might cause it to emit event(s) in a new transaction.
          //    For example, EventStream.fromSeq(1, 2, 3).startWith(0) will schedule three events in three transactions
          // 4. Now that the next signal's current value is initialized, we can update SwitchSignal's current value to match
          //    IMPORTANT: This will be done IMMEDIATELY, without any delay because we're already inside `trx`.
          //    Conversely, any events scheduled in point (3) above will run AFTER `trx` is done.
          //    This is as desired. It lets SwitchSignal emit the next signal's initial value before
          //    emitting subsequent updates to it that might have been triggered by starting it.
          //    Prior to Airstream 0.15.0 the next signal's initial value would have been missed in such cases.

          nextSignalTry.foreach(Protected.maybeWillStart)

          fireTry(nextSignalTry.flatMap(_.tryNow()), trx) // #Note[onStart,trx,loop]

          // If we're receiving events, this signal is started, so no need to check for that
          nextSignalTry.foreach { nextSignal =>
            nextSignal.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false)
          }
        })
      }
    }
  }

  // #Note this overrides default SingleParentSignal implementation
  override protected def onWillStart(): Unit = {
    Protected.maybeWillStart(parent)
    currentSignalTry.foreach(Protected.maybeWillStart)
    updateCurrentValueFromParent()
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    currentSignalTry.foreach(_.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false))
  }

  override protected[this] def onStop(): Unit = {
    removeInternalObserverFromCurrentSignal()
    super.onStop()
  }

  private def removeInternalObserverFromCurrentSignal(): Unit = {
    currentSignalTry.foreach { currentSignal =>
      currentSignal.removeInternalObserver(internalEventObserver)
    }
  }
}
