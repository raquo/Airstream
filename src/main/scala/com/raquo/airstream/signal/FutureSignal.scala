package com.raquo.airstream.signal

import com.raquo.airstream.core.Transaction

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

/** This signal behaves a bit differently than other signals typically do:
  * it keeps track of state regardless of whether it is started.
  * This is possible because this case requires no special memory management.
  */
class FutureSignal[A](
  future: Future[A]
) extends Signal[Option[A]] {

  override protected[airstream] val topoRank: Int = 1

  override protected[this] def initialValue: Option[A] = future.value match {
    case None => None
    case Some(Success(value)) => Some(value)
    case Some(Failure(e)) => throw e
  }

  if (!future.isCompleted) {
    future.onComplete {
      case Success(newValue) => new Transaction(fire(Some(newValue), _))
      case Failure(e) => throw e
    }
  }
}
