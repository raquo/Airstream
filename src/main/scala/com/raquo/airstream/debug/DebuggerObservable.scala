package com.raquo.airstream.debug

import com.raquo.airstream.common.InternalTryObserver
import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, Observable}

import scala.util.Try

/** See [[DebuggableObservable]] and [[DebuggableSignal]] for user-facing debug methods */
trait DebuggerObservable[A] extends InternalTryObserver[A] {

  protected val debugger: Debugger[A]

  protected[this] def debugFireTry(nextValue: Try[A]): Unit = {
    try {
      debugger.onFire(nextValue)
    } catch {
      case err: Throwable =>
        val maybeCause = nextValue.toEither.left.toOption
        AirstreamError.sendUnhandledError(DebugError(err, cause = maybeCause))
    }
  }

  protected[this] def debugOnStart(): Unit = {
    try {
      debugger.onStart()
    } catch {
      case err: Throwable => AirstreamError.sendUnhandledError(DebugError(err, cause = None))
    }
  }

  protected[this] def debugOnStop(): Unit = {
    try {
      debugger.onStop()
    } catch {
      case err: Throwable => AirstreamError.sendUnhandledError(DebugError(err, cause = None))
    }
  }
}

object DebuggerObservable {

  def defaultDisplayName[A](parent: Observable[A]): String = {
    parent match {
      case _: DebuggerObservable[_] =>
        // #TODO[UX] This could be confusing. But the alternative (|Debug|Debug|Debug names) is annoying.
        // When chaining multiple debug observables, they will inherit the parent's displayName
        parent.displayName
      case _ =>
        // We need to indicate that this isn't the original observable, but a debugged one,
        // otherwise debugging could get really confusing
        s"${parent.displayName}|Debug"
    }
  }
}
