package com.raquo.airstream.flatten

import com.raquo.airstream.common.{ InternalNextErrorObserver, SingleParentObservable }
import com.raquo.airstream.core.{ Observable, Transaction, WritableEventStream }

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue // #TODO #nc remove this in 15.0.0
import scala.concurrent.Future

/** This stream emits the values that the parent observables' emitted futures resolve with,
  * in the order in which they resolve (which is likely different from the order in which the futures are emitted).
  *
  * @param dropPreviousValues if enabled, this option makes this stream NOT emit values of futures that were emitted
  *                           earlier than a future that has already resolved. So if the parent stream emits
  *                           three futures and the third one resolves before the first two, this stream will NOT emit
  *                           the values of the first two futures when they resolve.
  *                           This option is useful for applications such as autocomplete results
  */
class ConcurrentFutureStream[A](
  protected[this] val parent: Observable[Future[A]],
  dropPreviousValues: Boolean,
  emitIfFutureCompleted: Boolean
) extends WritableEventStream[A] with SingleParentObservable[Future[A], A] with InternalNextErrorObserver[Future[A]] {

  // @TODO[Integrity] We should probably eventually deal with the vars' overflow issue

  private[this] var lastFutureIndex: Int = 0

  private[this] var lastEmittedValueIndex: Int = 0

  override protected val topoRank: Int = 1

  override protected def onNext(nextFuture: Future[A], transaction: Transaction): Unit = {
    lastFutureIndex += 1
    val nextFutureIndex = lastFutureIndex
    if (!nextFuture.isCompleted || emitIfFutureCompleted) {
      nextFuture.onComplete { nextValue =>
        if (!dropPreviousValues || (nextFutureIndex > lastEmittedValueIndex)) {
          lastEmittedValueIndex = nextFutureIndex
          // @TODO[API] Should lastEmittedValueIndex be updated only on success or also on failure?
          //println(s"> init trx from ConcurrentFutureStream.onNext")
          new Transaction(fireTry(nextValue, _))
        }
      }
    }
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    lastFutureIndex += 1
    val nextFutureIndex = lastFutureIndex
    if (!dropPreviousValues || (nextFutureIndex > lastEmittedValueIndex)) {
      lastEmittedValueIndex = nextFutureIndex
      // @TODO[API] Should lastEmittedValueIndex be updated only on success or also on failure?
      // @TODO[Performance] We use future.onComplete to better match the timing of onNext. Perhaps this is a bit overkill.
      Future.failed(nextError).onComplete(_ => new Transaction(fireError(nextError, _)))
    }
  }
}
