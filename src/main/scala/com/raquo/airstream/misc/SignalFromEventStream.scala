package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{EventStream, Protected, Transaction, WritableSignal}

import scala.util.Try

class SignalFromEventStream[A](
  override protected[this] val parent: EventStream[A],
  lazyInitialValue: => Try[A]
) extends WritableSignal[A] with SingleParentObservable[A, A] with InternalTryObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def initialValue: Try[A] = lazyInitialValue

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    fireTry(nextValue, transaction)
  }
}
