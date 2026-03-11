package com.raquo.airstream.scan

import com.raquo.airstream.core.Observable

import scala.util.Try

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
   *                    Exceptions thrown during evaluation are caught by Airstream (see `recover()`).
   * @see               [[scanLeftRecover]], [[reduceLeft]]
   */
  def scanLeft[B](
    initial: => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: (B, A) => B,
  ): ScanSelf[B] = {
    val f = if (skipErrors) Recover.skipErrors(combine) else Recover.keepErrors(combine)
    scanLeftRecover(Try(initial), resetOnStop)(f)
  }

  /**
   * Accumulates all events or updates from this parent using `combine`.
   * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
   *
   * @param initial     The seed value for the accumulator.
   *                    For signals, this is combined with the signal's initial value immediately.
   *                    For streams, this is used as the initial value until the first event arrives.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
   * @param combine     A binary operator to update the accumulator given its previous value and the next event.
   *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
   * @see               [[scanLeft]]
   */
  def scanLeftRecover[B](
    initial: Try[B],
    resetOnStop: Boolean = false,
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
   *                    Exceptions thrown during evaluation are caught by Airstream (see `recover()`).
   * @see               [[scanLeft]]
   */
  def reduceLeft[B >: A](
    combine: (B, A) => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  ): ReduceSelf[B]
}
