package com.raquo.airstream.signal

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.SingleParentObservable

class SignalFromEventStream[A](
  override protected[this] val parent: EventStream[A],
  override protected[this] val initialValue: A
) extends Signal[A] with SingleParentObservable[A, A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected[airstream] def onNext(nextValue: A, transaction: Transaction): Unit = {
    fire(nextValue, transaction)
  }
}
