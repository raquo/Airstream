package com.raquo.airstream.debug

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, Protected, Signal, Transaction}

import scala.util.Try

/** See [[DebuggableObservable]] and [[DebuggableSignal]] for user-facing debug methods */
class DebuggerSignal[A](
  override protected[this] val parent: Signal[A],
  override protected val debugger: Debugger[A]
) extends SingleParentSignal[A, A] with DebuggerObservable[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def defaultDisplayName: String = DebuggerObservable.defaultDisplayName(parent)

  override protected def currentValueFromParent(): Try[A] = {
    val parentValue = parent.tryNow()
    try {
      debugger.onEvalFromParent(parentValue)
    } catch {
      case err: Throwable =>
        val maybeCause = parentValue.toEither.left.toOption
        AirstreamError.sendUnhandledError(DebugError(err, cause = maybeCause))
    }
    parentValue
  }

  override protected[this] def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    debugFireTry(nextValue)
    super.fireTry(nextValue, transaction)
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    debugOnStart()
    debugFireTry(tryNow())
  }

  override protected[this] def onStop(): Unit = {
    super.onStop()
    debugOnStop()
  }

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    super.onTry(nextParentValue, transaction)
    fireTry(nextParentValue, transaction)
  }
}
