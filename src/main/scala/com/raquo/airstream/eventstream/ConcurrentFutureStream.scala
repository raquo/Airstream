package com.raquo.airstream.eventstream

import com.raquo.airstream.core.{Observable, Transaction}
import com.raquo.airstream.features.SingleParentObservable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

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
  dropPreviousValues: Boolean
) extends EventStream[A] with SingleParentObservable[Future[A], A] {

  // @TODO[Integrity] We should probably eventually deal with the vars' overflow issue

  private[this] var lastFutureIndex: Int = 0

  private[this] var lastEmittedValueIndex: Int = 0

  override protected[airstream] val topoRank: Int = 1

  override protected[airstream] def onNext(nextValue: Future[A], transaction: Transaction): Unit = {
    lastFutureIndex += 1
    val nextFutureIndex = lastFutureIndex
    nextValue.onComplete { tryNextValue =>
      if (isStarted && (!dropPreviousValues || (nextFutureIndex > lastEmittedValueIndex))) {
        lastEmittedValueIndex = nextFutureIndex
        // @TODO[API] Should lastEmittedValueIndex be updated only on success or also on failure?
        tryNextValue match {
          case Success(value) => new Transaction(fire(value, _))
          case Failure(err) => throw err
        }
      }
    }
  }
}

