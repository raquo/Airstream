package com.raquo.airstream.debug

import com.raquo.airstream.common.SingleParentEventStream
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

import scala.util.{Failure, Success, Try}

/** See [[DebuggableObservable]] and [[DebuggableSignal]] for user-facing debug methods */
class DebuggerEventStream[A](
  override protected[this] val parent: EventStream[A],
  override protected val debugger: Debugger[A]
) extends SingleParentEventStream[A, A] with DebuggerObservable[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def defaultDisplayName: String = DebuggerObservable.defaultDisplayName(parent)

  override protected[this] def fireValue(nextValue: A, transaction: Transaction): Unit = {
    debugFireTry(Success(nextValue))
    super.fireValue(nextValue, transaction)
  }

  override protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    debugFireTry(Failure(nextError))
    super.fireError(nextError, transaction)
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    debugOnStart()
  }

  override protected[this] def onStop(): Unit = {
    super.onStop()
    debugOnStop()
  }

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    fireTry(nextParentValue, transaction)
  }
}
