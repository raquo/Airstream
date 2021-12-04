package com.raquo.airstream.common

import com.raquo.airstream.core.{InternalObserver, Observable, Protected, Transaction, WritableEventStream}

/** A simple stream that only has one parent. */
trait SingleParentEventStream[I, O] extends WritableEventStream[O] with InternalObserver[I] {

  protected[this] val parent: Observable[I]

  override protected def onWillStart(): Unit = {
    //println(s"${this} >>>> onWillStart")
    Protected.maybeWillStart(parent)
  }

  override protected[this] def onStart(): Unit = {
    //println(s"${this} >>>> onStart")
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    Transaction.removeInternalObserver(parent, observer = this)
    super.onStop()
  }
}
