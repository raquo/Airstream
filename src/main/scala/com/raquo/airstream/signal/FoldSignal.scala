package com.raquo.airstream.signal

import com.raquo.airstream.core.{Observable, Transaction}
import com.raquo.airstream.features.{InternalTryObserver, SingleParentObservable}

import scala.util.Try

/** Note: In folds, failure is often toxic to all subsequent events.
  *       You often can not satisfactorily recover from a failure downstream
  *       because you will not have access to previous non-failed state in `fn`
  *       Therefore, make sure to handle recoverable errors in `fn`.
  *
  * @param makeInitialValue Note: Must not throw!
  * @param fn Note: Must not throw!
  */
class FoldSignal[A, B](
  override protected[this] val parent: Observable[A],
  makeInitialValue: () => Try[B],
  fn: (Try[B], Try[A]) => Try[B]
) extends Signal[B] with SingleParentObservable[A, B] with InternalTryObserver[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected[airstream] def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    fireTry(fn(tryNow(), nextParentValue), transaction)
  }

  override protected[this] def initialValue: Try[B] = makeInitialValue()
}
