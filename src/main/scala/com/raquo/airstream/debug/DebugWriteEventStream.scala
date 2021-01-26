package com.raquo.airstream.debug

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentObservable}
import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, EventStream, Transaction}

import scala.util.{Failure, Success}

class DebugWriteEventStream[A](
  override protected val parent: EventStream[A],
  debugger: ObservableDebugger[A]
) extends DebugEventStream[A](debugger) with SingleParentObservable[A, A] with InternalNextErrorObserver[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected[airstream] def onNext(nextParentValue: A, transaction: Transaction): Unit = {
    fireValue(nextParentValue, transaction)
  }

  override protected[airstream] def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }

  override protected[this] def fireValue(nextValue: A, transaction: Transaction): Unit = {
    try {
      debugger.onFire(Success(nextValue))
    } catch {
      case err: Throwable => AirstreamError.sendUnhandledError(DebugError(err, cause = None))
    }
    super.fireValue(nextValue, transaction)
  }

  override protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    try {
      debugger.onFire(Failure(nextError))
    } catch {
      case err: Throwable => AirstreamError.sendUnhandledError(DebugError(err, cause = Some(nextError)))
    }
    super.fireError(nextError, transaction)
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
