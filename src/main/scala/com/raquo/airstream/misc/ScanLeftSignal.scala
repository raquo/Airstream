package com.raquo.airstream.misc

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Observable, Protected, Signal, Transaction}

import scala.util.Try

/** Note: In folds, failure is often toxic to all subsequent events.
  *       You often can not satisfactorily recover from a failure downstream
  *       because you will not have access to previous non-failed state in `fn`
  *       Therefore, make sure to handle recoverable errors in `fn`.
  *
  * @param makeInitialValue Note: Must not throw!
  * @param fn Note: Must not throw!
  */
class ScanLeftSignal[A, B](
  override protected[this] val parent: Observable[A],
  makeInitialValue: () => Try[B],
  fn: (Try[B], Try[A]) => Try[B]
) extends SingleParentSignal[A, B] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  /** #Note: this is called from tryNow(), make sure to avoid infinite loop. */
  override protected def currentValueFromParent(): Try[B] = {
    if (parentIsSignal) {
      val parentSignal = parent.asInstanceOf[Signal[A @unchecked]]
      maybeLastSeenCurrentValue
        .map(lastSeenCurrentValue => fn(lastSeenCurrentValue, parentSignal.tryNow()))
        .getOrElse(makeInitialValue())
    } else {
      maybeLastSeenCurrentValue
        .getOrElse(makeInitialValue())
    }
  }

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    super.onTry(nextParentValue, transaction)
    fireTry(fn(tryNow(), nextParentValue), transaction)
  }
}
