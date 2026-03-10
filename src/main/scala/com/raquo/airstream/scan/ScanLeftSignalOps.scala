package com.raquo.airstream.scan

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.scan.Recover.{Combine, CombineTry}

import scala.util.Try

/**
 * A base trait for [[Signal]] with reduction operators such as `scanLeft` and `reduceLeft`.
 *
 * @tparam Self The kind of signal that is created by all reductions.
 * @tparam A    The type of value contained in this signal.
 *
 * @note Implementations must define `scanLeftRecover`.
 * @see [[ScanLeftStreamOps]] for the corresponding operators on [[EventStream]].
 */
trait ScanLeftSignalOps[+Self[+B] <: Signal[B], +A] extends ScanLeftOps[Self, Self, A] {

  /**
   * Accumulates all emissions from this parent using a binary operator `fn`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param makeInitial A function for creating the seed value for the accumulator given the initial value of this parent.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param skipErrors  Whether to skip error-valued emissions, omitting them from the accumulation (default is `false`).
   *                    If `false`, errors will persist until restart.
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `fn` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting signal.
   * @return            A [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @see [[scanLeftRecover]] has manual error handling, and [[reduceLeft]] has no `initial` seed value.
   */
  @inline final def scanLeft[B](
    makeInitial: A => B,
    resetOnStop: Boolean,
    skipErrors: Boolean,
  )(
    combine: Combine[A, B],
  ): Self[B] = {
    val f = if (skipErrors) Recover.skipErrors(combine) else Recover.keepErrors(combine)
    scanLeftRecover(_.map(makeInitial), resetOnStop = resetOnStop)(f)
  }

  /**
   * Accumulates all emissions from this parent using a binary operator `fn`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param makeInitial A function for creating the seed value for the accumulator given the initial value of this parent.
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `fn` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting signal.
   * @return            A [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @see [[scanLeftRecover]] has manual error handling, and [[reduceLeft]] has no `initial` seed value.
   */
  // Note: This is needed as default parameters are not supported on more than one method of the same name.
  @inline final def scanLeft[B](
    makeInitial: A => B,
  )(
    combine: Combine[A, B],
  ): Self[B] = {
    scanLeft(makeInitial, resetOnStop = false, skipErrors = false)(combine)
  }

  /**
   * Accumulates all emissions from this parent using a binary operator `fn`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param makeInitial A function for creating the seed value for the accumulator given the initial value of this parent.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `fn` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting signal.
   * @return            A [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @see [[scanLeft]] has automatic error handling, and [[reduceLeft]] has no `initial` seed value.
   */
  def scanLeftRecover[B](
    makeInitial: Try[A] => Try[B],
    resetOnStop: Boolean,
  )(
    combine: CombineTry[A, B],
  ): Self[B]

  /**
   * Accumulates all emissions from this parent using a binary operator `fn`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param makeInitial A function for creating the seed value for the accumulator given the initial value of this parent.
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `fn` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting signal.
   * @return            A [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @see [[scanLeft]] has automatic error handling, and [[reduceLeft]] has no `initial` seed value.
   */
  // Note: This is needed as default parameters are not supported on more than one method of the same name.
  @inline final def scanLeftRecover[B](
    makeInitial: Try[A] => Try[B],
  )(
    combine: CombineTry[A, B],
  ): Self[B] = {
    scanLeftRecover(makeInitial, resetOnStop = false)(combine)
  }

  @inline final override def scanLeftRecover[B](
    initial: Try[B],
    resetOnStop: Boolean,
  )(
    combine: CombineTry[A, B],
  ): Self[B] = {
    scanLeftRecover[B](combine(initial, _), resetOnStop)(combine)
  }

  @inline final override def reduceLeft[B >: A](
    resetOnStop: Boolean,
    skipErrors: Boolean,
  )(
    combine: Combine[A, B],
  ): Self[B] = {
    scanLeft[B](identity[B])(combine)
  }

  /**
   * Accumulates all emissions from this parent using a binary operator `fn`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is not safe for `fn` to throw uncaught exceptions; you must use [[Try]] instead!
   * @tparam B          The type of the accumulated value and thus of the resulting signal.
   * @return            A [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @see [[reduceLeft]] has automatic error handling, amd [[scanLeft]] has an explicit initial seed value.
   */
  @inline final def reduceLeftRecover[B >: A](
    resetOnStop: Boolean = false,
  )(
    combine: CombineTry[A, B],
  ): Self[B] = {
    scanLeftRecover(identity[Try[B]])(combine)
  }

  /**
   * Accumulates all emissions from this parent using a binary operator `fn`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param combine A binary operator that takes a tuple of the previously accumulated value and
   *                the next emission from this parent to produce the next accumulated value.
   *                It is not safe for `fn` to throw uncaught exceptions; you must use [[Try]] instead!
   * @tparam B      The type of the accumulated value and thus of the resulting signal.
   * @return        A [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @see [[reduceLeft]] has automatic error handling, amd [[scanLeft]] has an explicit initial seed value.
   */
  // Note: This is provided so that users don't have to pass an empty parameter list.
  @inline final def reduceLeftRecover[B >: A](
    combine: CombineTry[A, B],
  ): Self[B] = {
    reduceLeftRecover[B]()(combine)
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](makeInitial: A => B)(combine: Combine[A, B]): Self[B] = scanLeft[B](makeInitial)(combine)

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](makeInitial: A => B)(combine: CombineTry[A, B]): Self[B] = scanLeftRecover[B](makeInitial)(combine)
}
