package com.raquo.airstream.common

import com.raquo.airstream.core.{
  Observable,
  Protected,
  Signal,
  Transaction,
  WritableSignal
}

import scala.util.Try

/** A simple stream that only has one parent. */
trait SingleParentSignal[I, O]
    extends WritableSignal[O]
    with InternalTryObserver[I] {

  protected[this] val parent: Observable[I]

  protected[this] val parentIsSignal: Boolean = parent.isInstanceOf[Signal[_]]

  // Note: `-1` here means that we've never synced up with the parent.
  //       I am not sure if -1 vs 0 has a practical difference, but looking
  //       at our onWillStart code, it seems that using -1 here would be more
  //       prudent. If using 0, the initial onWillStart may not detect the
  //       "change" (from no value to parent signal's initial value), and the
  //       signal's value would only be updated in tryNow().
  protected[this] var _parentLastUpdateId: Int = -1

  /** Note: this is overriden in:
    *   - [[com.raquo.airstream.misc.SignalFromStream]] because parent can be
    *     stream, and it has cacheInitialValue logic
    *   - [[com.raquo.airstream.split.SplitChildSignal]] because its parent is a
    *     special timing stream, not the real parent
    */
  override protected def onWillStart(): Unit = {
    // dom.console.log(s"${this} > onWillStart (SPS)")
    Protected.maybeWillStart(parent)
    if (parentIsSignal) {
      val newParentLastUpdateId =
        Protected.lastUpdateId(parent.asInstanceOf[Signal[_]])
      if (newParentLastUpdateId != _parentLastUpdateId) {
        updateCurrentValueFromParent(
          currentValueFromParent(),
          newParentLastUpdateId
        )
      }
    }
  }

  /** Note: this is overridden in:
    *   - [[com.raquo.airstream.split.SplitChildSignal]] to clear cached initial
    *     value (if any)
    *   - [[com.raquo.airstream.distinct.DistinctSignal]] to filter out isSame
    *     events
    */
  protected def updateCurrentValueFromParent(
      nextValue: Try[O],
      nextParentLastUpdateId: Int
  ): Unit = {
    // dom.console.log(s"${this} > updateCurrentValueFromParent")
    setCurrentValue(nextValue)
    _parentLastUpdateId = nextParentLastUpdateId
  }

  /** Note: this is overridden in:
    *   - [[com.raquo.airstream.split.SplitChildSignal]] because its parent is a
    *     special timing stream, not the real parent
    */
  override protected def onTry(
      nextParentValue: Try[I],
      transaction: Transaction
  ): Unit = {
    if (parentIsSignal) {
      _parentLastUpdateId =
        Protected.lastUpdateId(parent.asInstanceOf[Signal[_]])
    }
  }

  override protected[this] def onStart(): Unit = {
    // println(s"${this} > onStart")
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    parent.removeInternalObserver(observer = this)
    super.onStop()
  }
}
