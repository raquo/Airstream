package com.raquo.airstream.distinct

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{EventStream, Protected, Transaction, WritableEventStream}

import scala.scalajs.js
import scala.util.Try

/** Emits only values that are distinct from the last emitted value, according to isSame function */
class DistinctEventStream[A](
  override protected val parent: EventStream[A],
  isSame: (Try[A], Try[A]) => Boolean
) extends WritableEventStream[A] with SingleParentObservable[A, A] with InternalTryObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  private var maybeLastSeenValue: js.UndefOr[Try[A]] = js.undefined

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    val distinct = maybeLastSeenValue.map(!isSame(_, nextValue)).getOrElse(true)
    maybeLastSeenValue = nextValue
    if (distinct) {
      fireTry(nextValue, transaction)
    }
  }
}
