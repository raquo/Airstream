package com.raquo.airstream.scan

import com.raquo.airstream.core.Observable

import scala.util.{Failure, Success, Try}

/**
  * A base trait for [[Observable]] with reduction operators such as `scanLeft` and `reduceLeft`.
  *
  * @tparam ScanSelf   The kind of observable that is created by `scanLeft`.
  * @tparam ReduceSelf The kind of observable that is created by `reduceLeft`.
  * @tparam A          The type of value emitted by this observable.
  * @see               [[ScanLeftSignalOps]], [[ScanLeftStreamOps]]
  */
trait ScanLeftOps[+ScanSelf[+B] <: Observable[B], +ReduceSelf[+B] <: Observable[B], +A] {

  /**
    * Accumulates all events or updates from this parent using `combine`.
    * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
    *
    * @param initial     The seed value for the accumulator.
    *                    For signals, this is combined with the signal's initial value immediately.
    *                    For streams, this is used as the initial value until the first event arrives.
    * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
    * @param skipErrors  Whether to continue after receiving an error.
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    Exceptions here are emitted as errors
    * @see               [[scanLeftRecover]], [[reduceLeft]]
    */
  def scanLeft[B](
    initial: => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: (B, A) => B,
  ): ScanSelf[B] = {
    scanLeftRecover(
      initial = Try(initial),
      resetOnStop = resetOnStop,
      skipErrors = skipErrors,
    )(keepErrors(combine))
  }

  /**
    * Accumulates all events or updates from this parent using `combine`.
    * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
    *
    * @param initial     The seed value for the accumulator.
    *                    For signals, this is combined with the signal's initial value immediately.
    *                    For streams, this is used as the initial value until the first event arrives.
    * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
   *  @param skipErrors  Whether to continue after receiving an error.
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
    * @see               [[scanLeft]]
    */
  def scanLeftRecover[B](
    initial: Try[B],
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: (Try[B], Try[A]) => Try[B],
  ): ScanSelf[B]

  /**
    * Accumulates all events or updates from this parent using `combine`.
    * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
    *
    * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
    * @param skipErrors  Whether to continue after receiving an error.
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    Exceptions here are emitted as errors
    * @see               [[scanLeft]]
    */
  def reduceLeft[B >: A](
    combine: (B, A) => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  ): ReduceSelf[B]

  /** Convert a reduction function into one that is error-aware and keeps all errors. */
  protected def keepErrors[X, Y](combine: (Y, X) => Y): (Try[Y], Try[X]) => Try[Y] = {
    case (Success(current), Success(next)) => Try(combine(current, next))
    case (Failure(error), _) => Failure(error)
    case (_, Failure(error)) => Failure(error)
  }
}
