package com.raquo.airstream.scan

import com.raquo.airstream.core.Signal

import scala.util.Try

/**
 * A base trait for [[Signal]] with reduction operators such as `scanLeft` and `reduceLeft`.
 *
 * @tparam Self The kind of signal that this is.
 * @tparam A    The type of value contained in this signal.
 * @see         [[ScanLeftStreamOps]]
 */
trait ScanLeftSignalOps[+Self[+B] <: Signal[B], +A] extends ScanLeftOps[Self, Self, A] {

  /**
   * Accumulates all updates from this parent using `combine`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param makeInitial A generator for the accumulator's seed, given the initial value of this parent.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
   * @param skipErrors  Whether to continue after receiving an error.
   * @param combine     A binary operator to update the accumulator given its previous value and the next event.
   *                    Exceptions thrown during evaluation are caught by Airstream (see `recover()`).
   * @see               [[scanLeftRecover]], [[reduceLeft]]
   */
  def scanLeft[B](
    makeInitial: A => B,
    resetOnStop: Boolean,
    skipErrors: Boolean,
  )(
    combine: (B, A) => B,
  ): Self[B] = {
    val f = if (skipErrors) Recover.skipErrors(combine) else Recover.keepErrors(combine)
    scanLeftRecover(_.map(makeInitial), resetOnStop = resetOnStop)(f)
  }

  /**
   * Accumulates all updates from this parent using `combine`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param makeInitial A generator for the accumulator's seed, given the initial value of this parent.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
   * @param combine     A binary operator to update the accumulator given its previous value and the next event.
   *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
   * @see               [[scanLeft]]
   */
  def scanLeftRecover[B](
    makeInitial: Try[A] => Try[B],
    resetOnStop: Boolean,
  )(
    combine: (Try[B], Try[A]) => Try[B],
  ): Self[B]

  override def scanLeftRecover[B](
    initial: Try[B],
    resetOnStop: Boolean,
  )(
    combine: (Try[B], Try[A]) => Try[B],
  ): Self[B] = {
    scanLeftRecover[B](combine(initial, _), resetOnStop)(combine)
  }

  override def reduceLeft[B >: A] (
    combine: (B, A) => B,
    resetOnStop: Boolean,
    skipErrors: Boolean,
  ): Self[B] = {
    scanLeft[B](identity[B], resetOnStop = resetOnStop, skipErrors = skipErrors)(combine)
  }

  /**
   * Accumulates all updates from this parent using `combine`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param combine     A binary operator to update the accumulator given its previous value and the next event.
   *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
   * @see               [[reduceLeft]], [[scanLeft]]
   */
  def reduceLeftRecover[B >: A](
    combine: (Try[B], Try[A]) => Try[B],
    resetOnStop: Boolean = false,
  ): Self[B] = {
    scanLeftRecover(identity[Try[B]])(combine)
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](makeInitial: A => B)(combine: (B, A) => B): Self[B] = {
    scanLeft[B](makeInitial, resetOnStop = false, skipErrors = false)(combine)
  }

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](makeInitial: Try[A] => Try[B])(combine: (Try[B], Try[A]) => Try[B]): Self[B] = {
    scanLeftRecover[B](makeInitial, resetOnStop = false)(combine)
  }
}
