package com.raquo.airstream.misc

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

import scala.scalajs.js
import scala.util.Try

class SignalFromStream[A](
  override protected val parent: EventStream[A],
  pullInitialValue: => Try[A],
  cacheInitialValue: Boolean
) extends SingleParentSignal[A, A] {

  private var hasEmittedEvents = false

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  // #Note: this overrides the default implementation
  override protected def onWillStart(): Unit = {
    Protected.maybeWillStart(parent)
    maybeCurrentValueFromParent.foreach(setCurrentValue)
  }

  override protected def currentValueFromParent(): Try[A] = {
    maybeCurrentValueFromParent.getOrElse(tryNow())
  }

  private def maybeCurrentValueFromParent: js.UndefOr[Try[A]] = {
    // #Note See also SplitChildSignal and CustomSignalSource for similar logic
    // #Note This can be called from inside tryNow(), so make sure to avoid an infinite loop
    if (maybeLastSeenCurrentValue.isEmpty) {
      // Signal has no current value – first time this is called
      pullInitialValue
    } else if (!hasEmittedEvents && !cacheInitialValue) {
      // Signal has current value, has not emitted yet, and we're pulling a fresh one
      // Essentially, we keep its value in sync with the `pullInitialValue` expression
      // on every restart. #TODO[API] Not sure if this is a good default, to be honest.
      pullInitialValue
    } else {
      js.undefined
    }
  }

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    hasEmittedEvents = true
    super.onTry(nextParentValue, transaction)
    fireTry(nextParentValue, transaction)
  }
}
