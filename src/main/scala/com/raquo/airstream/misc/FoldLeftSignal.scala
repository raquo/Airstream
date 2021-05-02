package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{Observable, Protected, Transaction, WritableSignal}

import scala.util.Try

/** Note: In folds, failure is often toxic to all subsequent events.
  *       You often can not satisfactorily recover from a failure downstream
  *       because you will not have access to previous non-failed state in `fn`
  *       Therefore, make sure to handle recoverable errors in `fn`.
  *
  * @param makeInitialValue Note: Must not throw!
  * @param fn Note: Must not throw!
  */
class FoldLeftSignal[A, B](
  override protected[this] val parent: Observable[A],
  makeInitialValue: () => Try[B],
  fn: (Try[B], Try[A]) => Try[B]
) extends WritableSignal[B] with SingleParentObservable[A, B] with InternalTryObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    fireTry(fn(tryNow(), nextParentValue), transaction)
  }

  override protected def initialValue: Try[B] = makeInitialValue()
}
