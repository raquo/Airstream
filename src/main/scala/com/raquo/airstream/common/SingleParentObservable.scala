package com.raquo.airstream.common

import com.raquo.airstream.core.{InternalObserver, Observable, Transaction}

/** A simple observable that only has one parent. */
trait SingleParentObservable[I, +O] extends Observable[O] with InternalObserver[I] {

  protected val parent: Observable[I]

  override protected def onStart(): Unit = {
    parent.addInternalObserver(this)
    super.onStart()
  }

  override protected def onStop(): Unit = {
    Transaction.removeInternalObserver(parent, observer = this)
    super.onStop()
  }
}
