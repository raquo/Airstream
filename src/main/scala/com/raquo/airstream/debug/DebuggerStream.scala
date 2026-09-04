package com.raquo.airstream.debug

import com.raquo.airstream.common.SingleParentStream
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

import scala.util.{Failure, Success, Try}

/** See [[DebugOps]] and [[DebuggableSignal]] for user-facing debug methods */
class DebuggerStream[A](
  override protected val parent: EventStream[A],
  override protected val debugger: Debugger[A]
) extends SingleParentStream[A, A] with DebuggerObservable[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def defaultDisplayName: String = DebuggerObservable.defaultDisplayName(parent)

  override protected def fireValue(nextValue: A, transaction: Transaction): Unit = {
    debugFireTry(Success(nextValue))
    super.fireValue(nextValue, transaction)
  }

  override protected def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    debugFireTry(Failure(nextError))
    super.fireError(nextError, transaction)
  }

  override protected def onStart(): Unit = {
    super.onStart()
    debugOnStart()
  }

  override protected def onStop(): Unit = {
    super.onStop()
    debugOnStop()
  }

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    fireTry(nextParentValue, transaction)
  }
}
