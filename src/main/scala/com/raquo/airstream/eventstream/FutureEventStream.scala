package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

/** This stream emits a value that the future resolves with.
  *
  * This stream will not emit any events to subscribers added after the
  * future was resolved, except as provided by `emitIfFutureCompleted`.
  *
  * @param emitIfFutureCompleted If false, this stream will emit an event when it's initialized with
  *                              an already completed future. Generally you should avoid this and use
  *                              [[com.raquo.airstream.signal.FutureSignal]] instead.
  */
class FutureEventStream[A](future: Future[A], emitIfFutureCompleted: Boolean) extends EventStream[A] {

  override protected[airstream] val topoRank: Int = 1

  if (emitIfFutureCompleted || !future.isCompleted) {
    future.onComplete {
      // @TODO[API] Do we need "isStarted" filter on these? Doesn't seem to affect anything for now...
      case Success(newValue) => new Transaction(fire(newValue, _))
      case Failure(e) => throw e
    }
  }
}
