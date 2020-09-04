package com.raquo.airstream.signal

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.{InternalTryObserver, SingleParentObservable}

import scala.util.Try

class SignalFromEventStream[A](
  override protected[this] val parent: EventStream[A],
  lazyInitialValue: => Try[A]
) extends Signal[A] with SingleParentObservable[A, A] with InternalTryObserver[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected def initialValue: Try[A] = lazyInitialValue

  override protected[airstream] def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    internal.fireTry(nextValue, transaction)
  }
}
