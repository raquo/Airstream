package com.raquo.airstream.scan

import com.raquo.airstream.core.{Observable, Signal}

import scala.util.Try

trait ScanLeftSignalOps[+Self[+_], +A] extends ScanLeftOps[Self, Self, A] {

  /**
   * Creates a [[Signal]] that combines all updates from the parent [[Signal]] using `fn`,
   * and emits the accumulated value every time that the parent [[Signal]] emits.
   *
   * @param makeInitial
   *   A function for creating the initial value to use before the first update,
   *   which takes the initial value of the parent [[Signal]] as input.
   *
   * @param fn
   * A binary operator that takes
   * the previously accumulated value (on the left) and
   * the next update from the parent [[Observable]] (on the right),
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
  @inline final def scanLeft[B](makeInitial: A => B)(fn: (B, A) => B): Self[B] = {
    scanLeftRecover[B](_.map(makeInitial))(recoverable(fn))
  }

  /**
   * Creates a [[Signal]] that combines all updates from the parent [[Observable]] using `fn`,
   * and emits the accumulated value every time that the parent stream emits.s
   *
   * @param makeInitial
   *   A function for creating the initial value to use before the first update,
   *   which takes the initial value of the parent [[Signal]] as input.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next update from the parent stream (on the right),
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
  def scanLeftRecover[B](
    makeInitial: Try[A] => Try[B]
  )(
    fn: (Try[B], Try[A]) => Try[B]
  ): Self[B]

  @inline final def scanLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Self[B] =
    scanLeftRecover[B](fn(initial, _))(fn)

  @inline final def reduceLeft[B >: A](fn: (B, A) => B): Self[B] = {
    scanLeft(identity[B])(fn)
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](makeInitial: A => B)(fn: (B, A) => B): Self[B] = scanLeft(makeInitial)(fn)

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](makeInitial: Try[A] => Try[B])(fn: (Try[B], Try[A]) => Try[B]): Self[B] = scanLeftRecover(makeInitial)(fn)
}
