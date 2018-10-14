package com.raquo.airstream.signal

import com.raquo.airstream.core.Transaction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

// @TODO confirm that memory management is ok here between the future and this signal.

/** This signal behaves a bit differently than other signals typically do:
  * it keeps track of state regardless of whether it is started.
  * This is possible because this case requires no special memory management.
  */
class FutureSignal[A](
  future: Future[A]
) extends Signal[Option[A]] {

  override protected[airstream] val topoRank: Int = 1

  override protected[this] def initialValue: Try[Option[A]] = future.value.fold[Try[Option[A]]](
    Success(None)
  )(
    value => value.map(Some(_))
  )

  if (!future.isCompleted) {
    future.onComplete(value => new Transaction(fireTry(value.map(Some(_)), _)))
  }
}
