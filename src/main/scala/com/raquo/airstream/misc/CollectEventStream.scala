package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentEventStream}
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

import scala.util.Try

/** This stream applies `fn` to the parent stream's events, and emits `x` from the resulting `Some(x)` value (if `None`, nothing is fired).
  *
  * This stream emits an error if the parent stream emits an error (Note: no filtering applied), or if `fn` throws
  *
  * @param fn Note: guarded against exceptions
  */
class CollectEventStream[A, B](
  override protected[this] val parent: EventStream[A],
  fn: A => Option[B],
) extends SingleParentEventStream[A, B] with InternalNextErrorObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onNext(nextParentValue: A, transaction: Transaction): Unit = {
    Try(fn(nextParentValue)).fold(
      onError(_, transaction),
      nextValue => nextValue.foreach(fireValue(_, transaction))
    )
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }
}
