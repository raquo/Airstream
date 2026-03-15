package com.raquo.airstream.scan

import com.raquo.airstream.core.{EventStream, Signal}

import scala.util.Try

/** A base trait for [[EventStream]] with reduction operators such as `scanLeft` and `reduceLeft`.
  *
  * @tparam A The type of event emitted by this stream.
  * @see      [[ScanLeftSignalOps]]
  */
trait ScanLeftStreamOps[+A] extends ScanLeftOps[Signal, EventStream, A] {

  /** Accumulates all events from this parent using `combine`.
    * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
    * Values are given as [[Option]], which are [[None]] precisely when this parent has not yet emitted.
    *
    * @param combine A binary operator to update the accumulator given its previous value and the next event.
   *                 Exceptions here are emitted as errors.
    * @see           [[reduceLeft]], [[scanLeft]]
    */
  private def reduceLeftOption[B >: A](
    combine: (B, A) => B,
  ): Signal[Option[B]] = {
    scanLeft[Option[B]](None) {
      case (None, next) => Some(next)
      case (Some(previous), next) => Some(combine(previous, next))
    }
  }

  override def reduceLeft[B >: A](
    combine: (B, A) => B,
  ): EventStream[B] = {
    reduceLeftOption[B](combine)
      .updates.collect {
        case Some(value) => value
      }
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](initial: B)(fn: (B, A) => B): Signal[B] = {
    scanLeft(initial)(fn)
  }

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    scanLeftRecover(initial)(fn)
  }
}
