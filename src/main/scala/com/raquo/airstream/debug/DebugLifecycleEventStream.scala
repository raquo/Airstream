package com.raquo.airstream.debug

import com.raquo.airstream.core.{AirstreamError, EventStream, Transaction}
import com.raquo.airstream.features.{InternalNextErrorObserver, SingleParentObservable}

import scala.util.Try

/** This stream helps you debug when parent stream is started / stopped.
  *
  * Use this stream as a replacement for the parent stream for it to work.
  *
  * Note: exceptions in provided callbacks will be sent directly to unhandled errors
  */
class DebugLifecycleEventStream[A](
  override protected val parent: EventStream[A],
  start: () => Unit,
  stop: () => Unit
) extends EventStream[A] with SingleParentObservable[A, A] with InternalNextErrorObserver[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected[airstream] def onNext(nextParentValue: A, transaction: Transaction): Unit = {
    fireValue(nextParentValue, transaction)
  }

  override protected[airstream] def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()
    Try(start()).recover { case err => AirstreamError.sendUnhandledError(err) }
  }

  override protected[this] def onStop(): Unit = {
    super.onStop()
    Try(stop()).recover { case err => AirstreamError.sendUnhandledError(err) }
  }
}
