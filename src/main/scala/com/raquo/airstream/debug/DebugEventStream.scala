package com.raquo.airstream.debug

import com.raquo.airstream.core.{EventStream, Transaction}

import scala.util.{Failure, Success}

/** See [[DebuggableObservable]] and [[DebuggableSignal]] for user-facing debug methods */
class DebugEventStream[A](
  override protected val parent: EventStream[A],
  override protected val debugger: ObservableDebugger[A]
) extends EventStream[A] with DebugObservable[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

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
}
