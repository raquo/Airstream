package com.raquo.airstream.timing

import com.raquo.airstream.core.{ Transaction, WritableSignal }
import com.raquo.airstream.state.StrictSignal

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue // #TODO #nc remove this in 15.0.0
import scala.concurrent.Future
import scala.util.{ Success, Try }

// @TODO confirm that memory management is ok here between the future and this signal.

/** This signal behaves a bit differently than other signals typically do:
  * it keeps track of state regardless of whether it is started.
  * This is possible because this case requires no special memory management.
  *
  * Note that being a StrictSignal, this exposes `now` and `tryNow` methods,
  * however if the `future` was not yet completed when this signal was created,
  * this signal's current value will be updated *asynchronously* after the future
  * has completed.
  */
class FutureSignal[A](
  future: Future[A]
) extends WritableSignal[Option[A]] with StrictSignal[Option[A]] {

  override protected val topoRank: Int = 1

  override protected[this] def initialValue: Try[Option[A]] = {

    val futureValue = future.value.fold[Try[Option[A]]](
      Success(None)
    )(
      value => value.map(Some(_))
    )

    // Subscribing to this signal, or requesting now() or tryNow() will trigger initialValue
    // evaluation, which will register an onComplete callback on the future if it's not resolved yet.

    // @nc @TODO If implementing https://github.com/raquo/Airstream/issues/43
    //      This needs to be adjusted to avoid more than one onComplete calls per instance of signal.
    //      Just add a boolean (don't look at tryNow, because that might cause infinite loop)

    if (!future.isCompleted) {
      future.onComplete(value => {
        //println(s"> init trx from FutureSignal($value)")
        new Transaction(fireTry(value.map(Some(_)), _))
      })
    }

    futureValue
  }
}
