package com.raquo.airstream.flatten

import com.raquo.airstream.common.{InternalTryObserver, SingleParentEventStream}
import com.raquo.airstream.core.{EventStream, InternalObserver, Protected, Signal, Transaction}

import scala.scalajs.js
import scala.util.{Success, Try}

/** This flattens an EventStream [ Signal[A] ] into an EventStream[A]
  *
  * When this stream is started, it re-emits the current value of the last signal emitted by `parent`
  * (including any updates to it, until it switches to the next signal).
  */
class SwitchSignalStream[A](
  override protected[this] val parent: EventStream[Signal[A]]
) extends SingleParentEventStream[Signal[A], A] with InternalTryObserver[Signal[A]] {

  override protected val topoRank: Int = 1

  private[this] var maybeCurrentSignalTry: js.UndefOr[Try[Signal[A]]] = js.undefined

  private[this] val internalEventObserver: InternalObserver[A] = InternalObserver.fromTry[A](
    onTry = (nextTry, _) => {
      //println(s"> init trx from SwitchSignalStream.onValue($nextTry)")
      new Transaction(fireTry(nextTry, _))
    }
  )

  override protected def onTry(nextSignalTry: Try[Signal[A]], transaction: Transaction): Unit = {
    switchToSignal(nextSignalTry)
  }

  private def switchToSignal(
    nextSignalTry: Try[Signal[A]]
  ): Unit = {
    val isSameSignal = maybeCurrentSignalTry.fold(false) { currentSignalTry =>
      (nextSignalTry, currentSignalTry) match {
        case (Success(nextSignal), Success(currentSignal)) => nextSignal == currentSignal
        case _ => false
      }
    }

    if (isSameSignal) {
      // We want to re-emit the signal's current value even if switching to the same signal to be consistent
      // with signals not having a built-in `==` check (since 0.15.0)
      //println(s"> init trx from SwitchSignalStream.onTry (same signal)")
      new Transaction(fireTry(nextSignalTry.flatMap(_.tryNow()), _)) // #Note[onStart,trx,loop]

    } else {
      removeInternalObserverFromCurrentSignal()
      maybeCurrentSignalTry = nextSignalTry


      //println(s"> init trx from SwitchSignalStream.onTry (new signal)")
      new Transaction(trx => {

        // #Note: Timing is important here.
        // 1. Create the `trx` transaction, since we need that boundary when flattening
        // 2. Ensure next signal is started by adding an internal observer to it
        // 3. Starting the signal might cause it to emit event(s) in a new transaction.
        //    For example, EventStream.fromSeq(1, 2, 3).startWith(0) will schedule three events in three transactions
        // 4. Now that the next signal's current value is initialized, we can have SwitchSignalStream emit the value
        //    IMPORTANT: This will be done IMMEDIATELY, without any delay because we're already inside `trx`.
        //    Conversely, any events scheduled in point (3) above will run AFTER `trx` is done.
        //    This is as desired. It lets SwitchSignalStream emit the next signal's initial value before
        //    emitting subsequent updates to it that might have been triggered by starting it.

        nextSignalTry.foreach(Protected.maybeWillStart)

        fireTry(nextSignalTry.flatMap(_.tryNow()), trx) // #Note[onStart,trx,loop]

        // If we're receiving events, this signal is started, so no need to check for that
        nextSignalTry.foreach { nextSignal =>
          nextSignal.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false)
        }
      })
    }
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    maybeCurrentSignalTry.foreach(_.foreach { currentSignal =>
      currentSignal.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false)
    })
  }

  override protected[this] def onStop(): Unit = {
    removeInternalObserverFromCurrentSignal()
    super.onStop()
  }

  private def removeInternalObserverFromCurrentSignal(): Unit = {
    maybeCurrentSignalTry.foreach(_.foreach { currentSignal =>
      currentSignal.removeInternalObserver(internalEventObserver)
    })
  }
}
