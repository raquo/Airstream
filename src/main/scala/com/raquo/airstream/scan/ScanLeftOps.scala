package com.raquo.airstream.scan

import com.raquo.airstream.core.{EventStream, Signal, Observable}
import com.raquo.airstream.state.StrictSignal
import org.scalajs.dom.MouseEvent

import scala.util.Try

/**
 * The base type for [[Observable]]s that have reduction operators such as `scanLeft`.
 * This is a parent of both [[ScanLeftSignalOps]] and [[ScanLeftEventOps]].
 *
 * @tparam Scan
 *   The kind of [[Observable]] that is created by `scanLeft` (e.g. [[Signal]] or [[StrictSignal]].)
 *
 * @tparam Reduce
 *   The kind of [[Observable]] that is created by `reduceLeft` (e.g. [[EventStream]].)
 *
 * @tparam A
 *   The type of value emitted by this [[Observable]]. (e.g. [[MouseEvent]] or [[Int]].)
 */
trait ScanLeftOps[+Scan[+_], +Reduce[+_], +A] {

  // @TODO[API] Should we introduce some kind of FoldError() wrapper?
  /**
   * Creates a [[Signal]] that combines all emissions from the parent [[Observable]] using `fn`,
   * and emits the accumulated value every time that the parent [[Observable]] emits.
   *
   * @param initial
   *   The initial value to use before the first emission from the parent.
   *   If the parent is a [[Signal]], the starting signal value will be combined with this parameter
   *   using `fn` to produce the new initial value.
   *
   * @param fn
   * A binary operator that takes
   * the previously accumulated value (on the left) and
   * the next emission from the parent [[Observable]] (on the right),
   * and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   A [[Signal]] that emits the accumulated value every time that the parent [[Observable]] emits.
   *
   * @note
   *   It is safe for `fn` or `initial` to throw an exception,
   *   in which case it will be propagated through the error channel of the resulting [[Signal]].
   *
   * @see
   *   [[scanLeftRecover]] for a version of this method with manual error recovery.
   *
   * @see
   *   [[reduceLeft]] for a version of this method without an initial value.
   */
  def scanLeft[B](initial: => B)(fn: (B, A) => B): Scan[B]

  /**
   * Creates a [[Signal]] that combines all emissions from the parent [[Observable]] using `fn`,
   * and emits the accumulated value every time that the parent stream emits.
   *
   * @param initial
   *   The initial value to use before the first emission from the parent.
   *   If the parent is a [[Signal]], the starting signal value will be combined with this parameter
   *   using `fn` to produce the new initial value.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next emission from the parent stream (on the right),
   *   and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   A [[Signal]] that emits the accumulated value every time that the parent [[Observable]] emits.
   *
   * @note
   *   It is not safe for `fn` to throw an exception.
   *   It is assumed that the user is responsible for wrapping any exceptions in [[Try]].
   *   If an uncaught exception nevertheless occurs, Airstream will likely crash.
   *
   * @see
   *   [[scanLeft]] for a version of this method that guards against exceptions,
   *   the use of which is recommended instead if you don't need manual error recovery.
   *
   * @see
   *   [[reduceLeftRecover]] for a version of this method without an initial value.
   */
  @inline final def scanLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Scan[B] =
    scanLeft(initial.get)(unrecoverable(fn))

  /**
   * Creates an [[Observable]] that combines all emissions from the parent [[Observable]] using `fn`,
   * and emits the accumulated value every time that the parent [[Observable]] emits.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next emission from the parent [[Observable]] (on the right),
   *   and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   An [[Observable]] that emits the accumulated value every time that the parent [[Observable]] emits.
   *
   * @note
   *   It is safe for `fn` to throw an exception,
   *   in which case it will be propagated through the error channel of the resulting [[Observable]].
   *
   * @see
   *   [[reduceLeftRecover]] for a version of this method with manual error recovery.
   *
   * @see
   *   [[scanLeft]] for a version of this method with an explicit initial value.
   */
  def reduceLeft[B >: A](fn: (B, A) => B): Reduce[B]

  /**
   * Creates an [[Observable]] that combines all emissions from the parent [[Observable]] using `fn`,
   * and emits the accumulated value every time that the parent [[Observable]] emits.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next emission from the parent [[Observable]] (on the right),
   *   and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   An [[Observable]] that emits the accumulated value every time that the parent [[Observable]] emits.
   *
   * @note
   *   It is not safe for `fn` to throw an exception.
   *   It is assumed that the user is responsible for wrapping any exceptions in [[Try]].
   *   If an uncaught exception nevertheless occurs, Airstream will likely crash.
   *
   * @see
   *   [[reduceLeft]] for a version of this method that guards against exceptions,
   *   the use of which is recommended instead if you don't need manual error recovery.
   *
   * @see
   *   [[scanLeftRecover]] for a version of this method with an explicit initial value.
   */
  @inline final def reduceLeftRecover[B >: A](fn: (Try[B], Try[A]) => Try[B]): Reduce[B] =
    reduceLeft(unrecoverable(fn))

  @inline protected final def recoverable[X, Y](fn: (Y, X) => Y)(current: Try[Y], next: Try[X]): Try[Y] = {
    Try(fn(current.get, next.get))
  }

  @inline protected final def unrecoverable[X, Y](fn: (Try[Y], Try[X]) => Try[Y])(current: Y, next: X): Y = {
    fn(Try(current), Try(next)).get
  }
}
