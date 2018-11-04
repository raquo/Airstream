package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.features.{InternalNextErrorObserver, SingleParentObservable}

import scala.util.Try

// @TODO[API] Should we also offer a Try[A] => Boolean filter? Currently handled by .collect.recover combination
/** This stream fires a subset of the events fired by its parent
  *
  * This stream emits an error if the parent stream emits an error (Note: no filtering applied), or if `passes` throws
  *
  * @param passes Note: guarded against exceptions
  */
class FilterEventStream[A](
  override protected val parent: EventStream[A],
  passes: A => Boolean
) extends EventStream[A] with SingleParentObservable[A, A] with InternalNextErrorObserver[A] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected[airstream] def onNext(nextParentValue: A, transaction: Transaction): Unit = {
    // @TODO[Performance] Can / should we replace internal Try()-s with try-catch blocks?
    Try(passes(nextParentValue)).fold(
      onError(_, transaction),
      passes => if (passes) fireValue(nextParentValue, transaction)
    )
  }

  override protected[airstream] def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }
}
