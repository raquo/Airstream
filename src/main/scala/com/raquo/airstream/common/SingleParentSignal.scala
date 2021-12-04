package com.raquo.airstream.common

import com.raquo.airstream.core.{InternalObserver, Observable, Protected, Transaction, WritableSignal}

import scala.util.Try

/** A simple stream that only has one parent. */
trait SingleParentSignal[I, O] extends WritableSignal[O] with InternalObserver[I] {

  protected[this] val parent: Observable[I]

  override protected def onWillStart(): Unit = {
    //println(s"${this} >>>> onWillStart")
    Protected.maybeWillStart(parent)
    updateCurrentValueFromParent()
  }

  /** Note: this is overridden in:
   *  - [[com.raquo.airstream.distinct.DistinctSignal]]
   *  - [[com.raquo.airstream.flatten.SwitchSignal]]
   */
  protected def updateCurrentValueFromParent(): Try[O] = {
    //println(s"${this} >> updateCurrentValueFromParent")
    val nextValue = currentValueFromParent()
    setCurrentValue(nextValue)
    //println(s"${this} << updateCurrentValueFromParent")
    nextValue
  }

  override protected[this] def onStart(): Unit = {
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    Transaction.removeInternalObserver(parent, observer = this)
    super.onStop()
  }
}
