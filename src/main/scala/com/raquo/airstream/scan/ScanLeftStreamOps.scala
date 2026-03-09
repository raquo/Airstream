package com.raquo.airstream.scan

import com.raquo.airstream.core.{EventStream, Observable, Signal}

import scala.util.Try

trait ScanLeftStreamOps[+A] extends ScanLeftOps[Signal, EventStream, A] {

  /**
   * Creates a [[Signal]] that combines all events from the parent [[EventStream]] using `fn`,
   * and emits the accumulated value every time that the parent [[EventStream]] emits.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next event from the parent [[EventStream]] (on the right),
   *   and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   A [[Signal]] that emits the accumulated value every time that the parent [[EventStream]] emits.
   *   The contents are wrapped in [[Option]],
   *   which is [[None]] precisely when the parent [[EventStream]] has not emitted anything yet.
   *
   * @note
   *   It is safe for `fn` to throw an exception,
   *   in which case it will be propagated through the error channel of the resulting [[Observable]].
   *
   * @see
   *   [[reduceLeftOptionRecover]] for a version of this method with manual error recovery.
   *
   * @see
   *   [[reduceLeft]] for a version of this method where the result isn't wrapped in [[Option]].
   */
  @inline final def reduceLeftOption[B >: A](fn: (B, A) => B): Signal[Option[B]] = {
    scanLeft[Option[B]](None) {
      case (None, next) => Some(next)
      case (Some(previous), next) => Some(fn(previous, next))
    }
  }

  /**
   * Creates a [[Signal]] that combines all events from the parent [[EventStream]] using `fn`,
   * and emits the accumulated value every time that the parent [[EventStream]] emits.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next event from the parent [[EventStream]] (on the right),
   *   and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   A [[Signal]] that emits the accumulated value every time that the parent [[EventStream]] emits.
   *   The contents are wrapped in [[Option]],
   *   which is [[None]] precisely when the parent [[EventStream]] has not emitted anything yet.
   *
   * @note
   *   It is not safe for `fn` to throw an exception.
   *   It is assumed that the user is responsible for wrapping any exceptions in [[Try]].
   *   If an uncaught exception nevertheless occurs, Airstream will likely crash.
   *
   * @see
   *   [[reduceLeftOption]] for a version of this method that guards against exceptions,
   *   the use of which is recommended instead if you don't need manual error recovery.
   *
   * @see
   *   [[reduceLeftRecover]] for a version of this method where the result isn't wrapped in [[Option]].
   */
  // @inline final def reduceLeftOptionRecover[B >: A](fn: (Try[B], Try[A]) => Try[B]): Signal[Option[B]] = {
  //  reduceLeftOption(unrecoverable(fn))
  // }

  /**
   * Creates a [[Signal]] that combines all events from the parent [[EventStream]] using `fn`,
   * and emits the accumulated value every time that the parent [[EventStream]] emits.
   *
   * @param default
   *   The default value to use before the first event from the parent [[EventStream]].
   *   After the first event, the default value is ignored and the accumulated value is emitted instead.
   *   The default value is not incorporated into the accumulated value in any way.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next event from the parent [[EventStream]] (on the right),
   *   and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   A [[Signal]] that emits the accumulated value every time that the parent [[EventStream]] emits.
   *   The `default` value is used until the first event arrives.
   *
   * @note
   *   It is safe for `fn` to throw an exception,
   *   in which case it will be propagated through the error channel of the resulting [[Observable]].
   *
   * @see
   *   [[reduceLeftDefaultRecover]] for a version of this method with manual error recovery.
   *
   * @see
   *   [[reduceLeft]] for a version of this method where no default is required.
   *
   * @see
   *   [[scanLeft]] for a version of this method where the default value is used to seed the accumulator.
   */
  @inline final def reduceLeftDefault[B >: A](default: => B)(fn: (B, A) => B): Signal[B] = {
    reduceLeftOption(fn).map(_.getOrElse(default))
  }

  /**
   * Creates a [[Signal]] that combines all events from the parent [[EventStream]] using `fn`,
   * and emits the accumulated value every time that the parent [[EventStream]] emits.
   *
   * @param default
   *   The default value to use before the first event from the parent [[EventStream]].
   *   After the first event, the default value is ignored and the accumulated value is emitted instead.
   *   The default value is not incorporated into the accumulated value in any way.
   *
   * @param fn
   *   A binary operator that takes
   *   the previously accumulated value (on the left) and
   *   the next event from the parent [[EventStream]] (on the right),
   *   and produces the next accumulated value.
   *
   * @tparam B
   *   The type of the accumulated value and the resulting [[Signal]].
   *
   * @return
   *   A [[Signal]] that emits the accumulated value every time that the parent [[EventStream]] emits.
   *   The `default` value is used until the first event arrives.
   *
   * @note
   *   It is not safe for `fn` to throw an exception.
   *   It is assumed that the user is responsible for wrapping any exceptions in [[Try]].
   *   If an uncaught exception nevertheless occurs, Airstream will likely crash.
   *
   * @see
   *   [[reduceLeftDefault]] for a version of this method with manual error recovery.
   *
   * @see
   *   [[reduceLeftRecover]] for a version of this method where no default is required.
   *
   * @see
   *   [[scanLeftRecover]] for a version of this method where the default value is used to seed the accumulator.
   */
  // @inline final def reduceLeftDefaultRecover[B >: A](default: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
  //  reduceLeftDefault(default.get)(unrecoverable(fn))
  // }

  @inline final def reduceLeft[B >: A](fn: (B, A) => B): EventStream[B] = {
    reduceLeftOption(fn).updates.collect { case Some(value) => value }
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](initial: B)(fn: (B, A) => B): Signal[B] = scanLeft(initial)(fn)

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = scanLeftRecover(initial)(fn)
}
