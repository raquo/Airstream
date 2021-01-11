package com.raquo.airstream.timing

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{EventStream, SyncObservable, Transaction}

import scala.scalajs.js
import scala.util.Try

class SyncDelayEventStream[A] (
  override protected[this] val parent: EventStream[A],
  after: EventStream[_]
) extends EventStream[A] with SingleParentObservable[A, A] with InternalTryObserver[A] with SyncObservable[A] {

  private[this] var maybePendingValue: js.UndefOr[Try[A]] = js.undefined

  override protected[airstream] val topoRank: Int = js.Math.max(parent.topoRank, after.topoRank) + 1

  override protected[airstream] def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    if (!transaction.pendingObservables.contains(this)) {
      // println(s"Marking SyncDelayEventStream($id) as pending in TRX(${transaction.id})")
      transaction.pendingObservables.enqueue(this)
    }
    maybePendingValue = nextValue
  }

  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    maybePendingValue.foreach { pendingValue =>
      maybePendingValue = js.undefined
      fireTry(pendingValue, transaction)
    }
  }

}
