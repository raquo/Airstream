package com.raquo.airstream.timing

import com.raquo.airstream.common.{InternalTryObserver, SingleParentEventStream}
import com.raquo.airstream.core.{Observable, Protected, SyncObservable, Transaction}

import scala.scalajs.js
import scala.util.Try

/** Note: This is generally supposed to be used only with streams as inputs.
  * Make sure you know what you're doing if using signals.
  * - if `parent` is a Signal, this stream mirrors `parent.changes`, not `parent`.
  * - if `after` is a Signal, this stream ignores its initial value
  */
class SyncDelayEventStream[A] (
  override protected[this] val parent: Observable[A],
  after: Observable[_]
) extends SingleParentEventStream[A, A] with InternalTryObserver[A] with SyncObservable[A] {

  private[this] var maybePendingValue: js.UndefOr[Try[A]] = js.undefined

  override protected val topoRank: Int = Protected.maxTopoRank(parent :: after :: Nil) + 1

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    if (!transaction.pendingObservables.contains(this)) {
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
