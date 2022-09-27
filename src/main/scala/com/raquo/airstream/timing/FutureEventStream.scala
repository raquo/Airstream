package com.raquo.airstream.timing

import com.raquo.airstream.core.{ Transaction, WritableEventStream }

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue // #TODO #nc remove this in 15.0.0
import scala.concurrent.Future

/** This stream emits a value that the future resolves with.
  *
  * This stream will not emit any events to subscribers added after the
  * future was resolved, except as provided by `emitIfFutureCompleted`.
  *
  * @param future Note: guarded against failures
  * @param emitIfFutureCompleted If false, this stream will emit an event when it's initialized with
  *                              an already completed future. Generally you should avoid this and use
  *                              [[FutureSignal]] instead.
  */
class FutureEventStream[A](future: Future[A], emitIfFutureCompleted: Boolean) extends WritableEventStream[A] {

  override protected val topoRank: Int = 1

  if (!future.isCompleted || emitIfFutureCompleted) {
    // @TODO[API] Do we need "isStarted" filter on these? Doesn't seem to affect anything for now...
    future.onComplete(_.fold(
      nextError => {
        //println(s"> init trx from FutureEventStream.init($nextError)")
        new Transaction(fireError(nextError, _))
      },
      nextValue => {
        //println(s"> init trx from FutureEventStream.init($nextValue)")
        new Transaction(fireValue(nextValue, _))
      }
    ))
  }
}
