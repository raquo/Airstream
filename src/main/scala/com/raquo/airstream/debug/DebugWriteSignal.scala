package com.raquo.airstream.debug

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, Signal, Transaction}

import scala.util.Try

class DebugWriteSignal[A](
  override protected val parent: Signal[A],
  debugger: ObservableDebugger[A]
) extends DebugSignal[A](debugger) with SingleParentObservable[A, A] with InternalTryObserver[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

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

  override protected[airstream] def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    fireTry(nextParentValue, transaction)
  }

  override protected[this] def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    try {
      debugger.onFire(nextValue)
    } catch {
      case err: Throwable =>
        val maybeCause = nextValue.toEither.left.toOption
        AirstreamError.sendUnhandledError(DebugError(err, cause = maybeCause))
    }
    super.fireTry(nextValue, transaction)
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    try {
      debugger.onStart()
    } catch {
      case err: Throwable => AirstreamError.sendUnhandledError(DebugError(err, cause = None))
    }
  }

  override protected[this] def onStop(): Unit = {
    super.onStop()
    try {
      debugger.onStop()
    } catch {
      case err: Throwable => AirstreamError.sendUnhandledError(DebugError(err, cause = None))
    }
  }
}
