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

  override def reduceLeft[B >: A](
    combine: (B, A) => B,
  ): Self[B] = {
    scanLeftGenerated[B](
      makeInitial = x => x,
    )(
      combine = combine,
    )
  }

  /** Accumulates all updates from this parent using `combine`.
    * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
    *
    * @param combine A binary operator to update the accumulator given its previous value and the next event.
   *                 It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
    * @see           [[reduceLeft]], [[scanLeftGenerated]]
    */
  def reduceLeftRecover[B >: A](
    combine: (Try[B], Try[A]) => Try[B],
  ): Self[B] = {
    scanLeftGeneratedRecover[B](
      makeInitial = x => x,
    )(
      combine = combine,
    )
  }

  /** Accumulates all updates from this parent using `combine`.
    * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
    *
    * @param makeInitial A generator for the accumulator's seed, given the initial value of this parent.
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    Exceptions here are emitted as errors.
    * @see               [[scanLeft]], [[scanLeftGeneratedRecover]], [[reduceLeft]]
    */
  def scanLeftGenerated[B](
    makeInitial: A => B,
  )(
    combine: (B, A) => B,
  ): Self[B] = {
    scanLeftGeneratedRecover[B](
      makeInitial = _.map(makeInitial),
      resumeOnError = true,
    )(
      combine = keepErrors(combine),
    )
  }

  /** Accumulates all updates from this parent using `combine`.
    * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
    *
    * @param makeInitial A generator for the accumulator's seed, given the initial value of this parent.
    * @param combine     A binary operator to update the accumulator given its previous value and the next event.
    *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
    * @see               [[scanLeftGenerated]], [[scanLeft]]
    */
  def scanLeftGeneratedRecover[B](
    makeInitial: Try[A] => Try[B],
  )(
    combine: (Try[B], Try[A]) => Try[B],
  ): Self[B] = {
    scanLeftGeneratedRecover(
      makeInitial = makeInitial,
      resumeOnError = false,
    )(
      combine = combine,
    )
  }

  protected override def scanLeftRecover[B](
    initial: Try[B],
    resumeOnError: Boolean,
  )(
    combine: (Try[B], Try[A]) => Try[B],
  ): Self[B] = {
    scanLeftGeneratedRecover[B](
      makeInitial = combine(initial, _),
      resumeOnError = resumeOnError,
    )(
      combine = combine,
    )
  }

  protected def scanLeftGeneratedRecover[B](
    makeInitial: Try[A] => Try[B],
    resumeOnError: Boolean,
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
