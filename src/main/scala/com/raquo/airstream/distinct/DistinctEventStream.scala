package com.raquo.airstream.distinct

import com.raquo.airstream.common.{InternalTryObserver, SingleParentEventStream}
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

import scala.scalajs.js
import scala.util.Try

/** Emits only values that are distinct from the last emitted value, according to isSame function */
class DistinctEventStream[A](
  override protected[this] val parent: EventStream[A],
  isSame: (Try[A], Try[A]) => Boolean,
  resetOnStop: Boolean
) extends SingleParentEventStream[A, A] with InternalTryObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  private var maybeLastSeenValue: js.UndefOr[Try[A]] = js.undefined

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    val isDistinct = maybeLastSeenValue.map(!isSame(_, nextValue)).getOrElse(true)
    maybeLastSeenValue = nextValue
    if (isDistinct) {
      fireTry(nextValue, transaction)
    }
  }

  override protected[this] def onStop(): Unit = {
    if (resetOnStop) {
      maybeLastSeenValue = js.undefined
    }
    super.onStop()
  }
}
