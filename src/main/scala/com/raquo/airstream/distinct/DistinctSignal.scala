package com.raquo.airstream.distinct

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{Protected, Signal, Transaction, WritableSignal}

import scala.util.Try

/** Emits only values that are distinct from the last emitted value, according to isSame function */
class DistinctSignal[A](
  override protected val parent: Signal[A],
  isSame: (Try[A], Try[A]) => Boolean
) extends WritableSignal[A] with SingleParentObservable[A, A] with InternalTryObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    if (!isSame(tryNow(), nextValue)) {
      fireTry(nextValue, transaction)
    }
  }

  override protected def initialValue: Try[A] = parent.tryNow()
}
