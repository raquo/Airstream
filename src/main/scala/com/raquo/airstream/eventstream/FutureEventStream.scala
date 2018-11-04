package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** This stream emits a value that the future resolves with.
  *
  * This stream will not emit any events to subscribers added after the
  * future was resolved, except as provided by `emitIfFutureCompleted`.
  *
  * @param future Note: guarded against failures
  * @param emitIfFutureCompleted If false, this stream will emit an event when it's initialized with
  *                              an already completed future. Generally you should avoid this and use
  *                              [[com.raquo.airstream.signal.FutureSignal]] instead.
  */
class FutureEventStream[A](future: Future[A], emitIfFutureCompleted: Boolean) extends EventStream[A] {

  override protected[airstream] val topoRank: Int = 1

  if (!future.isCompleted || emitIfFutureCompleted) {
    // @TODO[API] Do we need "isStarted" filter on these? Doesn't seem to affect anything for now...
    future.onComplete(_.fold(
      nextError => new Transaction(fireError(nextError, _)),
      nextValue => new Transaction(fireValue(nextValue, _))
    ))
  }
}
