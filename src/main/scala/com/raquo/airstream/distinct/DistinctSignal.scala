package com.raquo.airstream.distinct

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.{Protected, Signal, Transaction}

import scala.util.Try

/** Emits only values that are distinct from the last emitted value, according to isSame function */
class DistinctSignal[A](
  override protected[this] val parent: Signal[A],
  isSame: (Try[A], Try[A]) => Boolean,
  resetOnStop: Boolean
) extends SingleParentSignal[A, A] with InternalTryObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    if (!isSame(tryNow(), nextValue)) {
      fireTry(nextValue, transaction)
    }
  }

  override protected def currentValueFromParent(): Try[A] = parent.tryNow()

  /** Special implementation to add the distinct-ness filter */
  override protected def updateCurrentValueFromParent(): Try[A] = {
    val currentValue = tryNow()
    val nextValue = currentValueFromParent()
    // #Note We check this signal's standard distinction condition with !isSame instead of `==`
    //  because isSame might be something incompatible, e.g. reference equality
    if (resetOnStop || !isSame(nextValue, currentValue)) {
      setCurrentValue(nextValue)
      nextValue
    } else {
      currentValue
    }
  }
}
