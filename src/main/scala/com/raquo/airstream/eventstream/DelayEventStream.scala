package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.features.{InternalNextErrorObserver, SingleParentObservable}

import scala.scalajs.js

class DelayEventStream[A](
  override protected val parent: EventStream[A],
  delayMillis: Int
) extends EventStream[A] with SingleParentObservable[A, A] with InternalNextErrorObserver[A] {

  /** Async stream, so reset rank */
  override protected[airstream] val topoRank: Int = 1

  override protected[airstream] def onNext(nextValue: A, transaction: Transaction): Unit = {
    js.timers.setTimeout(delayMillis) {
      new Transaction(fireValue(nextValue, _))
    }
  }

  override def onError(nextError: Throwable, transaction: Transaction): Unit = {
    js.timers.setTimeout(delayMillis) {
      new Transaction(fireError(nextError, _))
    }
  }
}
