package com.raquo.airstream.common

import com.raquo.airstream.core.{Observable, Protected, Signal, Transaction, WritableSignal}

import scala.util.Try

/** A simple stream that only has one parent. */
trait SingleParentSignal[I, O] extends WritableSignal[O] with InternalTryObserver[I] {

  protected[this] val parent: Observable[I]

  protected[this] val parentIsSignal: Boolean = parent.isInstanceOf[Signal[_]]

  protected[this] var _parentLastUpdateId: Int = 0

  /** Note: this is overriden in:
    * - [[com.raquo.airstream.misc.SignalFromStream]] because parent can be stream, and it has cacheInitialValue logic
    */
  override protected def onWillStart(): Unit = {
    Protected.maybeWillStart(parent)
    if (parentIsSignal) {
      val newParentLastUpdateId = Protected.lastUpdateId(parent.asInstanceOf[Signal[_]])
      if (newParentLastUpdateId != _parentLastUpdateId) {
        updateCurrentValueFromParent()
      }
      _parentLastUpdateId = newParentLastUpdateId
    }
  }

  /** Note: this is overridden in:
   *  - [[com.raquo.airstream.distinct.DistinctSignal]]
   */
  protected def updateCurrentValueFromParent(): Unit = {
    val nextValue = currentValueFromParent()
    setCurrentValue(nextValue)
  }

  override protected def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
    if (parentIsSignal) {
      _parentLastUpdateId = Protected.lastUpdateId(parent.asInstanceOf[Signal[_]])
    }
  }

  override protected[this] def onStart(): Unit = {
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    parent.removeInternalObserver(observer = this)
    super.onStop()
  }
}
