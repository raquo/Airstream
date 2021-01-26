package com.raquo.airstream.debug

import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, Observer}

import scala.util.{Failure, Success, Try}

/** See [[DebuggableObserver]] for user-facing debug methods */
class DebuggerObserver[A](parent: Observer[A], debug: Try[A] => Unit) extends Observer[A] {

  override def defaultDisplayName: String = {
    parent match {
      case _: DebuggerObserver[_] =>
        // When chaining multiple debug observers, they will inherit the parent's displayName
        parent.displayName
      case _ =>
        // We need to indicate that this isn't the original observer, but a debugged one,
        // otherwise debugging could get really confusing
        s"${parent.displayName}|Debug"
    }
  }

  override def onTry(nextValue: Try[A]): Unit = {
    try {
      debug(nextValue)
    } catch {
      case err: Throwable =>
        val maybeCause = nextValue.toEither.left.toOption
        AirstreamError.sendUnhandledError(DebugError(err, cause = maybeCause))
    }
    parent.onTry(nextValue)
  }

  final override def onNext(nextValue: A): Unit = onTry(Success(nextValue))

  final override def onError(nextError: Throwable): Unit = onTry(Failure(nextError))
}
