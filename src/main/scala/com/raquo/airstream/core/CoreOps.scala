package com.raquo.airstream.core

trait CoreOps[+Self[+_], +A] {

  /** @param project Note: guarded against exceptions */
  def map[B](project: A => B): Self[B]

  /** `value` is passed by name, so it will be (re-)evaluated whenever a new event is processed.
    * You can use it to sample mutable values (e.g. myInput.ref.value in Laminar).
    *
    * See also: [[mapToStrict]]
    *
    * @param value Note: guarded against exceptions
    */
  def mapTo[B](value: => B): Self[B] = map(_ => value)

  /** `value` is evaluated strictly, only once, when this method is called.
    *
    * See also: [[mapTo]]
    */
  def mapToStrict[B](value: B): Self[B] = map(_ => value)

  def mapToUnit: Self[Unit] = map(_ => ())

  /** Execute a side effecting callback on every event.
    * If this is a Signal, the callback also runs when its initial value is evaluated.
    *
    * See https://github.com/raquo/Airstream/#tapEach for more details.
    */
  def tapEach[U](f: A => U): Self[A] = map { v => f(v); v }
}
