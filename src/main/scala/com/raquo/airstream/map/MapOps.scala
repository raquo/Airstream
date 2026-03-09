package com.raquo.airstream.map

import com.raquo.airstream.core.{EventStream, Signal}
import org.scalajs.dom.MouseEvent

/**
 * A base type for observables that have mapping operators such as `map` and `mapTo`.
 *
 * @tparam Self The kind of observable that this is,
 *              and that will be subsequently returned by the operators herein
 *              (e.g. [[Signal]] or [[EventStream]]).
 *
 * @tparam A The type of value emitted by this observable (e.g. [[MouseEvent]] or [[Int]]).
 */
trait MapOps[+Self[+_], +A] {

  /**
   * Map all emissions of this observable through a pure function `project`
   * that transforms the original value of type [[A]] into a new value of type [[B]].
   *
   * @param project The transformation by which emitted values are computed, based on the original value.
   *                It is safe for `project` to throw an exception,
   *                in which case the error will be emitted in the error channel of the resulting observable.
   *
   * @tparam B The type of value emitted by the resulting observable.
   *
   * @return A new observable that emits `project(x)` every time this observable emits a value `x`.
   *
   * @note By convention, the `project` function should be pure.
   *       However, this is not a strict requirement, and `project` will indeed be called exactly once per emission.
   *       Nevertheless, you are advised to instead use `tapEach` for executing side effects, to clarify intent.
   *
   * @see [[mapTo]] or [[mapToStrict]] for versions of this method where the new value doesn't depend on the underlying value.
   *
   * @see [[tapEach]] for a similar method that executes side effects instead of modifying the data.
   */
  def map[B](project: A => B): Self[B]

  /**
   * Map all emissions to a given value that doesn't depend on the underlying value.
   *
   * @param value The value to replace all emissions with.
   *              Evaluated lazily, once for every emission.
   *              You can use this to sample mutable values (e.g. [[myInput.ref.value]] in Laminar).
   *              It is safe for the evaluation of `value` to throw an exception,
   *              in which case the error will be emitted in the error channel of the resulting observable.
   *
   * @tparam B The type of the value with which all emissions are replaced.
   *
   * @return A new observable that computes and emits `value` every time this observable emits.
   *
   * @note Equivalent to `map(_ => value)` if `value` is defined with `def`.
   *       If `value` is pure, then this is also equivalent to `mapToStrict(value)`,
   *       with possibly different performance characteristics.
   *
   * @note The computation associated with any preceding transformations may still be performed,
   *       despite the fact that the original value is ultimately ignored.
   *
   * @see [[mapToStrict]] for a version of this method where `value` is evaluated once upfront.
   *
   * @see [[map]] for a generalisation whereby the new value can depend on the original under an arbitrary transformation.
   */
  def mapTo[B](value: => B): Self[B] = map(_ => value)

  /**
   * Map all emissions to a given constant that doesn't depend on the underlying value.
   *
   * @param value The value to replace all emissions with.
   *              Evaluated eagerly, only once, when this method is first called.
   *              It is not safe for the evaluation of `value` to throw an exception,
   *              in which case the error with be thrown immediately in the current call stack.
   *
   * @tparam B The type of the constant with which all emissions are replaced.
   *
   * @return A new observable that emits `value` every time this observable emits.
   *
   * @note Equivalent to `map(_ => value)` if `value` is defined with `val`.
   *       If `value` is pure, then this is also equivalent to `mapTo(value)`,
   *       with possibly different performance characteristics.
   *
   * @note The computation associated with any preceding transformations may still be performed,
   *       despite the fact that the original value is ultimately ignored.
   *
   * @see [[mapTo]] for a version of this method where `value` is evaluated anew for every emission.
   *
   * @see [[map]] for a generalisation whereby the new value can depend on the original under an arbitrary transformation.
   */
  def mapToStrict[B](value: B): Self[B] = map(_ => value)

  /**
   * Map all emissions to `()`, the only value of type [[Unit]].
   *
   * Useful when you want to ignore the value of an event, and just want to know when it happened.
   *
   * @return A new observable that emits `()` every time this observable emits.
   *
   * @note Equivalent to `mapTo(())` or `mapToStrict(())`.
   *
   * @see [[mapTo]] or [[mapToStrict]] for generalisations whereby the new value can be any constant rather than just `()`.
   */
  @inline def mapToUnit: Self[Unit] = map(_ => ())

  /**
   * Execute a side-effecting callback on every emission.
   *
   * @param f The side-effecting callback to execute on every emission.
   *          The value returned by `f` is completely ignored.
   *          It is safe for `f` to throw an exception,
   *          in which case the error will be emitted in the error channel of the resulting observable.
   *
   * @tparam U The type returned by the callback `f`, which is ignored.
   *
   * @return An observable equivalent to [[this]],
   *         except with the side-effecting callback `f` saved to be executed on every emission.
   *
   * @note If this is a [[Signal]], then `f` also runs when the initial value is evaluated.
   *
   * @note Calling [[tapEach]] on an observable is insufficient on its own.
   *       In order for the callback to actually execute,
   *       the resulting observable (or something downstream of it) must have an observer subscribed to it.
   *
   * @note `f` will be called exactly once per emission, provided that the resulting observable is observed.
   */
  @inline def tapEach[U](f: A => U): Self[A] = map { v => f(v); v }
}
