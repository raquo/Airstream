package com.raquo.airstream.flatten

import com.raquo.airstream.common.InternalTryObserver
import com.raquo.airstream.core.{InternalObserver, Protected, Signal, Transaction, WritableSignal}

import scala.scalajs.js
import scala.util.{Success, Try}

/** This flattens a Signal[ Signal[A] ] into a Signal[A]
  *
  * When this signal is started, its current value tracks the current value of the last signal emitted by `parent`.
  */
class SwitchSignal[A](
  parent: Signal[Signal[A]]
) extends WritableSignal[A] with InternalTryObserver[Signal[A]] {

  override protected val topoRank: Int = 1

  private[this] var maybeCurrentSignalTry: js.UndefOr[Try[Signal[A]]] = js.undefined

  private[this] var innerSignalLastSeenUpdateId: Int = 0

  private[this] def currentSignalTry: Try[Signal[A]] = maybeCurrentSignalTry.getOrElse {
    val initialSignal = parent.tryNow()
    maybeCurrentSignalTry = initialSignal
    initialSignal
  }

  private[this] val internalEventObserver: InternalObserver[A] = InternalObserver.fromTry[A](
    onTry = (nextTry, _) => {
      //println(s"> init trx from SwitchSignal.onValue($nextTry)")
      innerSignalLastSeenUpdateId = Protected.lastUpdateId(currentSignalTry.get)
      new Transaction(fireTry(nextTry, _))
    }
  )

  // #Note this is only used when getting the initial value of this signal
  override protected def currentValueFromParent(): Try[A] = {
    parent.tryNow().flatMap(_.tryNow())
  }

  override protected def onTry(nextParentValue: Try[Signal[A]], transaction: Transaction): Unit = {
    switchToSignalOnline(nextParentValue)
  }

  private def isSameSignal(signalTry1: Try[Signal[A]], signalTry2: Try[Signal[A]]): Boolean = {
    (signalTry1, signalTry2) match {
      case (Success(signal1), Success(signal2)) => signal1 eq signal2
      case _ => false
    }
  }

  override protected def onWillStart(): Unit = {
    Protected.maybeWillStart(parent)

    // If this SwitchSignal is starting, we just want to update its new current value,
    // we don't want to emit anything in a new transaction (if we did that instead, any
    // signals downstream of this signal would restart with a stale current value, and
    // then emit the updated value in that new transaction – that would be annoying,
    // I think). So, we simply call setCurrentValue(), and the new observers will pick
    // it up once this signal finishes starting.

    val nextSignalTry = parent.tryNow()

    if (isSameSignal(nextSignalTry, currentSignalTry)) {
      // println(" - same signal")
      currentSignalTry.foreach(Protected.maybeWillStart)
      val nextSignal = nextSignalTry.get // if isSameSignal is true, nextSignalTry is guaranteed Success()
      val nextSignalLastUpdateId = Protected.lastUpdateId(nextSignal)
      if (nextSignalLastUpdateId != innerSignalLastSeenUpdateId) {
        setCurrentValue(nextSignal.tryNow())
        innerSignalLastSeenUpdateId = nextSignalLastUpdateId
      }

    } else {
      // println(" - new signal")
      currentSignalTry.foreach(_.removeInternalObserver(internalEventObserver))
      maybeCurrentSignalTry = nextSignalTry

      nextSignalTry.foreach(Protected.maybeWillStart)
      setCurrentValue(nextSignalTry.flatMap(_.tryNow()))
      innerSignalLastSeenUpdateId = nextSignalTry.map(Protected.lastUpdateId).getOrElse(0)

      nextSignalTry.foreach(_.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false))
    }
  }

  def switchToSignalOnline(nextSignalTry: Try[Signal[A]]): Unit = {

    if (isSameSignal(nextSignalTry, currentSignalTry)) {
      // If we're switching to the same signal, it means that this same signal already has our internal observer,
      // has already been started, and we have already emitted its last known value. So there is nothing for us to do.
      // println("- same signal - do nothing")

    } else {

      currentSignalTry.foreach(_.removeInternalObserver(internalEventObserver))
      maybeCurrentSignalTry = nextSignalTry

      // This will be updated shortly in the transaction below
      // I don't think we should be setting a real value until we actually emit the corresponding value.
      innerSignalLastSeenUpdateId = 0

      // Update this signal's value with nextSignal's current value (or an error if we don't have nextSignal)

      //println(s"> init trx from SwitchSignal.onTry (new signal)")
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
        //    Prior to Airstream 15.0.0 the next signal's initial value would have been missed in such cases.

        // #TODO[Integrity] Is it possible for this SwitchSignal to stop after
        //  this transaction is created but before it executes, for some reason?
        //  - If no, then we don't need the isStarted check.
        if (isStarted) {
          nextSignalTry.foreach(Protected.maybeWillStart)

          fireTry(nextSignalTry.flatMap(_.tryNow()), trx) // #Note[onStart,trx,loop]

          innerSignalLastSeenUpdateId = nextSignalTry.map(Protected.lastUpdateId).getOrElse(0)

          nextSignalTry.foreach(_.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false))
        }
      })
    }
  }

  override protected[this] def onStart(): Unit = {
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)
    currentSignalTry.foreach(_.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false))
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    parent.removeInternalObserver(observer = this)
    currentSignalTry.foreach(_.removeInternalObserver(internalEventObserver))
    super.onStop()
  }

}
