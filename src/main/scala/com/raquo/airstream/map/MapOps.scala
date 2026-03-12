package com.raquo.airstream.map

import com.raquo.airstream.core.{EventStream, Signal}

/**
 * A base trait for observables that have mapping operators such as `map` and `mapTo`.
 * @tparam Self The kind of observable that this is (e.g. [[Signal]] or [[EventStream]]).
 * @tparam A The type of value emitted by this observable (e.g. [[org.scalajs.dom.MouseEvent]] or [[Int]]).
 */
trait MapOps[+Self[+_], +A] {

  /**
   * Map all events or updates of this observable through a pure transformation `project`.
   * The resulting observable emits once each time this parent observable emits.
   *
   * @param project The transformation to apply, to be called exactly once per event or update.
   *                Exceptions thrown during evaluation are caught by Airstream (see `recover()`).
   *
   * @see [[mapTo]], [[mapToStrict]], [[mapToUnit]], [[tapEach]]
   */
  def map[B](project: A => B): Self[B]

  /**
   * Ignore the current value and take a new value instead.
   *
   * @param value The replacement value, evaluated lazily and repeatedly on every event or update.
   *              Exceptions thrown during evaluation are caught by Airstream (see `recover()`).
   *
   * @note Equivalent to `map(_ => value)` if `value` is defined with `def`.
   * @see [[map]], [[mapToStrict]], [[mapToUnit]], [[tapEach]]
   */
  def mapTo[B](value: => B): Self[B] = map(_ => value)

  /**
   * Ignore the current value and take a new value instead.
   *
   * @param value The replacement value, evaluated eagerly and only once.
   *              Exceptions thrown during evaluation go uncaught in the current call stack.
   *
   * @note Equivalent to `map(_ => value)` if `value` is defined with `val`.
   * @see [[map]], [[mapTo]], [[mapToUnit]], [[tapEach]]
   */
  def mapToStrict[B](value: B): Self[B] = map(_ => value)

  /**
   * Ignore the current value and instead take the value "`()`" (i.e. of type [[Unit]]).
   *
   * @note Equivalent to `mapTo(())` or `mapToStrict(())`.
   * @see [[map]], [[mapTo]], [[mapToStrict]], [[tapEach]]
   */
  @inline def mapToUnit: Self[Unit] = map(_ => ())

  /**
   * Execute a side-effecting callback on every event or update.
   *
   * @param callback A callback whose return value is ignored, to be called exactly once per event or update.
   *                 Exceptions thrown during evaluation are caught by Airstream (see `recover()`).
   *
   * @note Will not work if no downstream observable is subscribed to by an observer.
   * @see [[map]], [[mapTo]], [[mapToStrict]], [[mapToUnit]]
   */
  @inline def tapEach[U](callback: A => U): Self[A] = map { v => callback(v); v }
}
