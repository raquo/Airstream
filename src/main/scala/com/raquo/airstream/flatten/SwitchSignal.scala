package com.raquo.airstream.flatten

import com.raquo.airstream.common.{ InternalTryObserver, SingleParentObservable }
import com.raquo.airstream.core.{ InternalObserver, Signal, Transaction, WritableSignal }

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

  private[this] var currentSignalTry: Try[Signal[A]] = parent.tryNow()

  private[this] val internalEventObserver: InternalObserver[A] = InternalObserver.fromTry[A](
    onTry = (nextTry, _) => {
      //println(s"> init trx from SwitchSignal.onValue($nextTry)")
      new Transaction(fireTry(nextTry, _))
    }
  )

  override protected def onTry(nextSignalTry: Try[Signal[A]], transaction: Transaction): Unit = {
    val isSameSignal = nextSignalTry.isSuccess && nextSignalTry == currentSignalTry
    if (!isSameSignal) {
      removeInternalObserverFromCurrentSignal()
      currentSignalTry = nextSignalTry

      // If we're receiving events, this signal is started, so no need to check for that
      nextSignalTry.foreach { nextSignal =>
        nextSignal.addInternalObserver(internalEventObserver)
      }
      //println(s"> init trx from SwitchSignal.onTry")
      // Update this signal's value with nextSignal's current value (or an error if we don't have nextSignal)
      new Transaction(fireTry(nextSignalTry.flatMap(_.tryNow()), _))
    }
  }

  override protected[this] def onStart(): Unit = {
    currentSignalTry.foreach(_.addInternalObserver(internalEventObserver))
    super.onStart()
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
