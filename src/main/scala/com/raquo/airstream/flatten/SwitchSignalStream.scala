package com.raquo.airstream.flatten

import com.raquo.airstream.common.InternalTryObserver
import com.raquo.airstream.core.{EventStream, InternalObserver, Protected, Signal, Transaction, WritableStream}

import scala.scalajs.js
import scala.util.{Success, Try}

/** This flattens an EventStream [ Signal[A] ] into an EventStream[A]
  *
  * When this stream is started, it re-emits the current value of the last signal emitted by `parent`
  * (including any updates to it, until it switches to the next signal).
  */
class SwitchSignalStream[A](
  parent: EventStream[Signal[A]]
) extends WritableStream[A] with InternalTryObserver[Signal[A]] {

  override protected val topoRank: Int = 1

  private[this] var maybeCurrentSignalTry: js.UndefOr[Try[Signal[A]]] = js.undefined

  private[this] var lastSeenSignalUpdateId: Int = 0

  private[this] val internalEventObserver: InternalObserver[A] = InternalObserver.fromTry[A](
    onTry = (nextTry, _) => {
      //println(s"> init trx from SwitchSignalStream.onValue($nextTry)")
      new Transaction(trx => {
        if (isStarted) {
          fireTry(nextTry, trx)
          maybeCurrentSignalTry.foreach { _.foreach { currentSignal =>
            lastSeenSignalUpdateId = Protected.lastUpdateId(currentSignal)
          }}
        }
      })
    }
  )

  override protected def onWillStart(): Unit = {
    Protected.maybeWillStart(parent)
    maybeCurrentSignalTry.foreach { _.foreach { currentSignal =>
      Protected.maybeWillStart(currentSignal)
    }}
  }

  override protected def onTry(nextSignalTry: Try[Signal[A]], transaction: Transaction): Unit = {
    switchToSignal(nextSignalTry)
  }

  private def switchToSignal(
    nextSignalTry: Try[Signal[A]]
  ): Unit = {
    val isSameSignal = maybeCurrentSignalTry.fold(false) { currentSignalTry =>
      (nextSignalTry, currentSignalTry) match {
        case (Success(nextSignal), Success(currentSignal)) => nextSignal eq currentSignal
        case _ => false
      }
    }

    if (isSameSignal) {
      // If we just emit the same signal, there is no need to re-emit its value, because this stream
      // has already emitted its current value, either while listening to it, or, if this stream was
      // stopped while this signal remained started, when this stream has restarted (it would have
      // checked if the signal emitted anything since the stream stopped).

    } else {
      removeInternalObserverFromCurrentSignal()
      maybeCurrentSignalTry = nextSignalTry
      // negative update ids don't exist normally, so next update is guaranteed to trigger an event.
      // we set this in case the transaction does not execute (not sure how this could happen though).
      lastSeenSignalUpdateId = -1

      //println(s"> init trx from SwitchSignalStream.onTry (new signal)")
      new Transaction(trx => {
        if (isStarted) {
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

          nextSignalTry.foreach { nextSignal =>
            lastSeenSignalUpdateId = Protected.lastUpdateId(nextSignal)
            nextSignal.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false)
          }
        }
      })
    }
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)
    maybeCurrentSignalTry.foreach(_.foreach { currentSignal =>
      val newSignalLastUpdateId = Protected.lastUpdateId(currentSignal)
      if (newSignalLastUpdateId != lastSeenSignalUpdateId) {
        //println(s"> init trx from SwitchSignalStream.onTry (same signal)")
        new Transaction(trx => {
          if (isStarted) {
            fireTry(currentSignal.tryNow(), trx) // #Note[onStart,trx,loop]
            lastSeenSignalUpdateId = newSignalLastUpdateId
          }
        })
      }
      currentSignal.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false)
    })
  }

  override protected[this] def onStop(): Unit = {
    removeInternalObserverFromCurrentSignal()
    parent.removeInternalObserver(observer = this)
    super.onStop()
  }

  private def removeInternalObserverFromCurrentSignal(): Unit = {
    maybeCurrentSignalTry.foreach { _.foreach { currentSignal =>
      currentSignal.removeInternalObserver(internalEventObserver)
    }}
  }
}
