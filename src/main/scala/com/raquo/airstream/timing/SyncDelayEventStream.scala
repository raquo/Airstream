package com.raquo.airstream.timing

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{EventStream, Protected, SyncObservable, Transaction, WritableEventStream}

import scala.scalajs.js
import scala.util.Try

class SyncDelayEventStream[A] (
  override protected[this] val parent: EventStream[A],
  after: EventStream[_]
) extends WritableEventStream[A] with SingleParentObservable[A, A] with InternalTryObserver[A] with SyncObservable[A] {

  private[this] var maybePendingValue: js.UndefOr[Try[A]] = js.undefined

  override protected val topoRank: Int = Protected.maxParentTopoRank(parent :: after :: Nil) + 1

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    if (!transaction.pendingObservables.contains(this)) {
       //println(s"Marking SyncDelayEventStream(${this.toString.substring(this.toString.indexOf('@'))}) as pending in TRX(${transaction.id})")
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
