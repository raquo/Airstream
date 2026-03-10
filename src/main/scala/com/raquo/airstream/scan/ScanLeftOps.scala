package com.raquo.airstream.scan

import com.raquo.airstream.core.Observable
import com.raquo.airstream.scan.Recover.{Combine, CombineTry}

import scala.util.Try

/**
 * A base trait for [[Observable]]s with reduction operators such as `scanLeft` and `reduceLeft`.
 *
 * @tparam Scan   The kind of observable that is created by `scanLeft`.
 * @tparam Reduce The kind of observable that is created by `reduceLeft`.
 * @tparam A      The type of value emitted by this observable.
 *
 * @note Implementations must define `scanLeftRecover` and `reduceLeft`.
 * @see  [[ScanLeftSignalOps]] and [[ScanLeftStreamOps]] for specialisations to signals and streams respectively.
 */
trait ScanLeftOps[+Scan[+B] <: Observable[B], +Reduce[+B] <: Observable[B], +A] {

  /**
   * Accumulates all emissions from this parent using a binary operator `combine`.
   * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
   *
   * @param initial     The seed value for the accumulator. This is evaluated eagerly in current call stack.
   *                    For signals, this is combined with the signal's initial value immediately.
   *                    For streams, this is used as the initial value until the first event arrives.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param skipErrors  Whether to skip error-valued emissions, omitting them from the accumulation (default is `false`).
   *                    If `false`, errors will persist until restart.
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `combine` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting observable.
   * @return            An [[Observable]] that emits the accumulated value every time this parent emits.
   *                    The kind of observable produced is typically a signal or derivative thereof.
   *
   * @see [[scanLeftRecover]] has manual error handling, and [[reduceLeft]] has no `initial` seed value.
   */
  @inline final def scanLeft[B](
    initial: => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: Combine[A, B],
  ): Scan[B] = {
    val f = if (skipErrors) Recover.skipErrors(combine) else Recover.keepErrors(combine)
    scanLeftRecover(Try(initial), resetOnStop)(f)
  }

  /**
   * Accumulates all emissions from this parent using a binary operator `combine`.
   * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
   *
   * @param initial     The seed value for the accumulator. This is evaluated eagerly in current call stack.
   *                    For signals, this is combined with the signal's initial value immediately.
   *                    For streams, this is used as the initial value until the first event arrives.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is not safe for `combine` to throw uncaught exceptions; you must use [[Try]] instead!
   * @tparam B          The type of the accumulated value and thus of the resulting observable.
   * @return            An [[Observable]] that emits the accumulated value every time this parent emits.
   *                    The kind of observable produced is typically a signal or derivative thereof.
   *
   * @see [[scanLeft]] has automatic error handling, and [[reduceLeft]] has no `initial` seed value.
   */
  @inline def scanLeftRecover[B](
    initial: Try[B],
    resetOnStop: Boolean = false,
  )(
    combine: CombineTry[A, B],
  ): Scan[B]

  /**
   * Accumulates all emissions from this parent using a binary operator `combine`.
   * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
   *
   *
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param skipErrors  Whether to skip error-valued emissions, omitting them from the accumulation (default is `false`).
   *                    If `false`, errors will persist until restart.
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `combine` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting observable.
   * @return            An [[Observable]] that emits the accumulated value every time this parent emits.
   *                    The kind of observable produced typically matches the kind of the parent observable.
   *
   * @see [[scanLeft]] has an explicit seed value.
   */
  def reduceLeft[B >: A](
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: Combine[A, B],
  ): Reduce[B]

  /**
   * Accumulates all emissions from this parent using a binary operator `combine`.
   * Produces an [[Observable]] that emits the accumulated value every time this parent emits.
   *
   * @param combine A binary operator that takes a tuple of the previously accumulated value and
   *                the next emission from this parent to produce the next accumulated value.
   *                It is safe for `combine` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B      The type of the accumulated value and thus of the resulting observable.
   * @return        An [[Observable]] that emits the accumulated value every time this parent emits.
   *                The kind of observable produced typically matches the kind of the parent observable.
   *
   * @see [[scanLeft]] has an explicit initial seed value.
   */
  // Note: This is provided so that users don't have to pass an empty parameter list.
  @inline final def reduceLeft[B >: A](
    combine: Combine[A, B],
  ): Reduce[B] = {
    reduceLeft[B]()(combine)
  }
}
