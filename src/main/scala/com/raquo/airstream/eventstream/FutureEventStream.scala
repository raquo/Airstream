package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

/** This stream emits a value that the future resolves with.
  *
  * Note: This stream does not emit any events to subscribers added after the
  * future was resolved. Use [[com.raquo.airstream.signal.FutureSignal]] for that.
  */
class FutureEventStream[A](future: Future[A]) extends EventStream[A] {

  override protected[airstream] val topoRank: Int = 1

  future.onComplete {
    // @TODO[API] Do we need "isStarted" filter on these? Doesn't seem to affect anything for now...
    case Success(newValue) => new Transaction(fire(newValue, _))
    case Failure(e) => throw e
  }
}
