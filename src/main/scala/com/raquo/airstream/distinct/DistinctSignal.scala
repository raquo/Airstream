package com.raquo.airstream.distinct

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Protected, Signal, Transaction}

import scala.util.Try

/** Emits only values that are distinct from the last emitted value, according to isSame function */
class DistinctSignal[A](
  override protected[this] val parent: Signal[A],
  isSame: (Try[A], Try[A]) => Boolean,
  resetOnStop: Boolean
) extends SingleParentSignal[A, A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    super.onTry(nextParentValue, transaction)
    if (!isSame(tryNow(), nextParentValue)) {
      fireTry(nextParentValue, transaction)
    }
  }

  override protected def currentValueFromParent(): Try[A] = parent.tryNow()

  /** Special implementation to add the distinct-ness filter */
  override protected def updateCurrentValueFromParent(): Unit = {
    // #TODO[Integrity] should I also check for lastUpdateId in addition to isSame?
    //  - if isSame, then it doesn't matter if the parent emitted, right? No event anyway.
    //  - if not isSame, then I don't think it's possible that the parent has NOT emitted,
    //    unless you're using some super weird isSame function where a != a.
    val nextValue = currentValueFromParent()
    // #Note We check this signal's standard distinction condition with !isSame instead of `==`
    //  because isSame might be something incompatible, e.g. reference equality
    if (resetOnStop || !isSame(nextValue, tryNow())) {
      setCurrentValue(nextValue)
    }
  }
}
