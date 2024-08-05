package com.raquo.airstream.common

import com.raquo.airstream.core.{
  InternalObserver,
  Observable,
  Protected,
  WritableStream
}

/** A simple stream that only has one parent. */
trait SingleParentStream[I, O]
    extends WritableStream[O]
    with InternalObserver[I] {

  protected[this] val parent: Observable[I]

  override protected def onWillStart(): Unit = {
    // println(s"${this} > onWillStart")
    Protected.maybeWillStart(parent)
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
