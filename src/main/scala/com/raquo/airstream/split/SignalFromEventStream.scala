package com.raquo.airstream.split

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{EventStream, Signal, Transaction}

import scala.util.Try

class SignalFromEventStream[A](
  override protected[this] val parent: EventStream[A],
  lazyInitialValue: => Try[A]
) extends Signal[A] with SingleParentObservable[A, A] with InternalTryObserver[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected def initialValue: Try[A] = lazyInitialValue

  override protected[airstream] def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    fireTry(nextValue, transaction)
  }
}
