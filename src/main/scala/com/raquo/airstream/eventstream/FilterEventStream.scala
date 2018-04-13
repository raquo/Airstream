package com.raquo.airstream.eventstream

import com.raquo.airstream.features.SingleParentObservable
import com.raquo.airstream.core.{Observer, Transaction}

/** This stream fires a subset of the events fired by its parent */
class FilterEventStream[A](
  override protected val parent: EventStream[A],
  passes: A => Boolean
) extends EventStream[A] with SingleParentObservable[A, A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected[airstream] def onNext(nextParentValue: A, transaction: Transaction): Unit = {
    if (passes(nextParentValue)) {
      fire(nextParentValue, transaction)
    }
  }
}
