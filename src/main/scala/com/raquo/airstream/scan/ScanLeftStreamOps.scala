package com.raquo.airstream.scan

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.scan.Recover.{Combine, CombineTry}

import scala.util.Try

/**
 * A base trait for [[EventStream]]s with reduction operators such as `scanLeft` and `reduceLeft`.
 * 
 * @tparam A The type of event emitted by this stream.
 *
 * @note Implementations must define `scanLeftRecover`.
 * @see  [[ScanLeftSignalOps]] for the corresponding operators on [[Signal]].
 */
trait ScanLeftStreamOps[+A] extends ScanLeftOps[Signal, EventStream, A] {

  /**
   * Accumulates all events from this parent using a binary operator `combine`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param skipErrors  Whether to skip error-valued emissions, omitting them from the accumulation (default is `false`).
   *                    If `false`, errors will persist until restart.
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `combine` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting signal.
   * @return            A [[Signal]] that emits the accumulated value every time this parent emits.
   *                    Values are given as [[Option]]s, which are [[None]] precisely when this parent has not yet emitted.
   *
   * @see [[reduceLeft]] instead produces an [[EventStream]], [[scanLeft]] has an explicit initial seed value.
   */
  @inline final def reduceLeftOption[B >: A](
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: Combine[A, B],
  ): Signal[Option[B]] = {
    scanLeft[Option[B]](None, resetOnStop = resetOnStop, skipErrors = skipErrors) {
      case (None, next) => Some(next)
      case (Some(previous), next) => Some(combine(previous, next))
    }
  }

  /**
   * Accumulates all events from this parent using a binary operator `combine`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param combine A binary operator that takes a tuple of the previously accumulated value and
   *                the next emission from this parent to produce the next accumulated value.
   *                It is safe for `combine` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B      The type of the accumulated value and thus of the resulting signal.
   * @return        A [[Signal]] that emits the accumulated value every time this parent emits.
   *                Values are given as [[Option]]s, which are [[None]] precisely when this parent has not yet emitted.
   *
   * @see [[reduceLeft]] instead produces an [[EventStream]], and [[scanLeft]] has an explicit initial seed value.
   */
  // Note: This is provided so that users don't have to pass an empty parameter list.
  @inline final def reduceLeftOption[B >: A](
    combine: Combine[A, B],
  ): Signal[Option[B]] = {
    reduceLeftOption[B]()(combine)
  }

  /**
   * Accumulates all events from this parent using a binary operator `combine`.
   * Produces a [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @param default     The initial value for the resulting signal, used until this parent emits for the first time.
   *                    Following the first event, `default` is discarded and thereafter plays no role.
   * @param resetOnStop Whether to reset the accumulator when this parent is restarted (default is `false`).
   * @param skipErrors  Whether to skip error-valued emissions, omitting them from the accumulation (default is `false`).
   *                    If `false`, errors will persist until restart.
   * @param combine     A binary operator that takes a tuple of the previously accumulated value and
   *                    the next emission from this parent to produce the next accumulated value.
   *                    It is safe for `combine` to throw uncaught exceptions, which are propagated through the error channel.
   * @tparam B          The type of the accumulated value and thus of the resulting signal.
   * @return            A [[Signal]] that emits the accumulated value every time this parent emits.
   *
   * @see [[reduceLeft]] has no initial value, and [[scanLeft]] folds the initial value into the accumulation.
   */
  @inline final def reduceLeftDefault[B >: A](
    default: => B,
    resetOnStop: Boolean = false,
    skipErrors: Boolean = false,
  )(
    combine: Combine[A, B],
  ): Signal[B] = {
    reduceLeftOption[B](resetOnStop = resetOnStop, skipErrors = skipErrors)(combine)
      .map(_.getOrElse(default))
  }

  @inline final override def reduceLeft[B >: A](
    resetOnStop: Boolean,
    skipErrors: Boolean,
  )(
    combine: Combine[A, B],
  ): EventStream[B] = {
    reduceLeftOption[B](resetOnStop = resetOnStop, skipErrors = skipErrors)(combine)
      .updates.collect { case Some(value) => value }
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](initial: B)(fn: Combine[A, B]): Signal[B] = scanLeft(initial)(fn)

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](initial: Try[B])(fn: CombineTry[A, B]): Signal[B] = scanLeftRecover(initial)(fn)
}
