package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.{Observable, Protected, Signal, Transaction}

import scala.scalajs.js
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
) extends SingleParentSignal[A, B] with InternalTryObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  private val parentIsSignal: Boolean = parent.isInstanceOf[Signal[_]]

  private var maybeLastSeenParentValue: js.UndefOr[Try[A]] = js.undefined

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    if (parentIsSignal) {
      maybeLastSeenParentValue = nextParentValue
    }
    fireTry(fn(tryNow(), nextParentValue), transaction)
  }

  /** #Note: this is called from tryNow(), make sure to avoid infinite loop. */
  override protected def currentValueFromParent(): Try[B] = {
    if (parentIsSignal) {
      val parentSignal = parent.asInstanceOf[Signal[A @unchecked]]
      val maybeUpdatedValue = for {
        lastSeenCurrentValue <- maybeLastSeenCurrentValue
        lastSeenParentValue <- maybeLastSeenParentValue
      } yield {
        val nextParentValue = parentSignal.tryNow()
        maybeLastSeenParentValue = nextParentValue
        if (nextParentValue != lastSeenParentValue) {
          fn(lastSeenCurrentValue, nextParentValue)
        } else {
          lastSeenCurrentValue
        }
      }
      maybeUpdatedValue.getOrElse {
        // #TODO Not great that we have a side effect here, but should be ok, I think
        maybeLastSeenParentValue = parentSignal.tryNow()
        makeInitialValue()
      }
    } else {
      maybeLastSeenCurrentValue.getOrElse(makeInitialValue())
    }
  }
}
