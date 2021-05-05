package com.raquo.airstream.debug

import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, Protected, Signal, Transaction, WritableSignal}

import scala.util.Try

/** See [[DebuggableObservable]] and [[DebuggableSignal]] for user-facing debug methods */
class DebuggerSignal[A](
  override protected val parent: Signal[A],
  override protected val debugger: Debugger[A]
) extends WritableSignal[A] with DebuggerObservable[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected[this] def initialValue: Try[A] = {
    val initial = parent.tryNow()
    try {
      debugger.onInitialEval(initial)
    } catch {
      case err: Throwable =>
        val maybeCause = initial.toEither.left.toOption
        AirstreamError.sendUnhandledError(DebugError(err, cause = maybeCause))
    }
    initial
  }

  override protected[this] def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    debugFireTry(nextValue)
    super.fireTry(nextValue, transaction)
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    debugOnStart()
  }

  override protected[this] def onStop(): Unit = {
    super.onStop()
    debugOnStop()
  }

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    fireTry(nextValue, transaction)
  }
}
