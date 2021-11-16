package com.raquo.airstream.flatten

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{InternalObserver, Signal, Transaction, WritableSignal}

import scala.scalajs.js
import scala.util.Try

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
) extends WritableSignal[A] with SingleParentObservable[Signal[A], A] with InternalTryObserver[Signal[A]] {

  override protected val topoRank: Int = 1

  override protected def initialValue: Try[A] = parent.tryNow().flatMap(_.tryNow())

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

  override protected def onTry(nextSignalTry: Try[Signal[A]], transaction: Transaction): Unit = {
    val isSameSignal = nextSignalTry.isSuccess && nextSignalTry == currentSignalTry

    if (isSameSignal) {
      // We want to re-emit the signal's current value in this case to be consistent
      // with signals not having a built-in `==` check (since 0.15.0)

      //println(s"> init trx from SwitchSignal.onTry (same signal)")
      new Transaction(fireTry(nextSignalTry.flatMap(_.tryNow()), _))

    } else {
      removeInternalObserverFromCurrentSignal()
      maybeCurrentSignalTry = nextSignalTry

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

        // If we're receiving events, this signal is started, so no need to check for that
        nextSignalTry.foreach { nextSignal =>
          nextSignal.addInternalObserver(internalEventObserver)
        }

        fireTry(nextSignalTry.flatMap(_.tryNow()), trx)
      })
    }
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    currentSignalTry.foreach(_.addInternalObserver(internalEventObserver))
  }

  override protected[this] def onStop(): Unit = {
    removeInternalObserverFromCurrentSignal()
    super.onStop()
  }

  private def removeInternalObserverFromCurrentSignal(): Unit = {
    currentSignalTry.foreach { currentSignal =>
      Transaction.removeInternalObserver(currentSignal, internalEventObserver)
    }
  }
}
