package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

import scala.util.Try

class SignalFromEventStream[A](
  override protected[this] val parent: EventStream[A],
  pullInitialValue: => Try[A],
  cacheInitialValue: Boolean
) extends SingleParentSignal[A, A] with InternalTryObserver[A] {

  private var hasEmittedEvents = false

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def currentValueFromParent(): Try[A] = {
    // #Note See also SplitChildSignal and CustomSignalSource for similar logic
    // #Note This can be called from inside tryNow(), so make sure to avoid an infinite loop
    if (maybeLastSeenCurrentValue.nonEmpty && (hasEmittedEvents || cacheInitialValue)) {
      tryNow()
    } else {
      pullInitialValue
    }
  }

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    hasEmittedEvents = true
    fireTry(nextValue, transaction)
  }
}
