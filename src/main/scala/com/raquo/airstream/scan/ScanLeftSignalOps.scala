package com.raquo.airstream.scan

import com.raquo.airstream.core.Signal

import scala.util.Try

/** A base trait for [[Signal]] with reduction operators such as `scanLeft` and `reduceLeft`.
  *
  * @tparam Self The kind of signal that this is.
  * @tparam A    The type of value contained in this signal.
  * @see         [[ScanLeftStreamOps]]
  */
trait ScanLeftSignalOps[+Self[+B] <: Signal[B], +A] extends ScanLeftOps[Self, Self, A] {

  override def scanLeftRecover[B](
    initial: Try[B],
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: (Try[B], Try[A]) => Try[B],
  ): Self[B] = {
    scanLeftGeneratedRecover[B](
      combine(initial, _),
      resetOnStop = resetOnStop,
      skipErrors = skipErrors,
    )(combine)
  }

  override def reduceLeft[B >: A](
    combine: (B, A) => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  ): Self[B] = {
    scanLeftGenerated[B](
      makeInitial = x => x,
      resetOnStop = resetOnStop,
      skipErrors = skipErrors,
    )(combine)
  }

  /** Accumulates all updates from this parent using `combine`.
    * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
    *
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
    * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
   *  @param skipErrors  Whether to continue after receiving an error.
    * @see               [[reduceLeft]], [[scanLeftGenerated]]
    */
  def reduceLeftRecover[B >: A](
    combine: (Try[B], Try[A]) => Try[B],
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  ): Self[B] = {
    scanLeftGeneratedRecover[B](
      makeInitial = x => x,
      resetOnStop = resetOnStop,
      skipErrors = skipErrors,
    )(combine)
  }

  /** Accumulates all updates from this parent using `combine`.
    * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
    *
    * @param makeInitial A generator for the accumulator's seed, given the initial value of this parent.
    * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
    * @param skipErrors  Whether to continue after receiving an error.
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    Exceptions here are emitted as errors
    * @see               [[scanLeft]], [[scanLeftGeneratedRecover]], [[reduceLeft]]
    */
  def scanLeftGenerated[B](
    makeInitial: A => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: (B, A) => B,
  ): Self[B] = {
    scanLeftGeneratedRecover[B](
      makeInitial = _.map(makeInitial),
      resetOnStop = resetOnStop,
      skipErrors = skipErrors,
    )(keepErrors(combine))
  }

  /** Accumulates all updates from this parent using `combine`.
    * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
    *
    * @param makeInitial A generator for the accumulator's seed, given the initial value of this parent.
    * @param resetOnStop Whether to reset the accumulator when this parent is restarted.
   *  @param skipErrors  Whether to continue after receiving an error.
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
    * @see               [[scanLeftGenerated]], [[scanLeft]]
    */
  def scanLeftGeneratedRecover[B](
    makeInitial: Try[A] => Try[B],
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: (Try[B], Try[A]) => Try[B],
  ): Self[B]

  @deprecated("foldLeft was renamed to scanLeft and later to scanLeftGenerated", "15.0.0-M1")
  def foldLeft[B](makeInitial: A => B)(combine: (B, A) => B): Self[B] = {
    scanLeftGenerated[B](makeInitial)(combine)
  }

  @deprecated("foldLeftRecover was renamed to scanLeftRecover and later to scanLeftGeneratedRecover", "15.0.0-M1")
  def foldLeftRecover[B](makeInitial: Try[A] => Try[B])(combine: (Try[B], Try[A]) => Try[B]): Self[B] = {
    scanLeftGeneratedRecover[B](makeInitial)(combine)
  }
}
