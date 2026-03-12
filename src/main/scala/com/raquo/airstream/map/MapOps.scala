package com.raquo.airstream.map

import com.raquo.airstream.core.{EventStream, Observable, Signal}

/**
  * A base trait for observables that have mapping operators such as [[map]] and [[mapTo]].
  *
  * @tparam Self The kind of observable that this is (e.g. [[EventStream]], [[Signal]], or [[com.raquo.airstream.state.StrictSignal]]).
  * @tparam A    The type of value emitted by this observable (e.g. [[org.scalajs.dom.MouseEvent]] or [[Int]]).
  */
trait MapOps[+Self[+_], +A] {

  /**
    * This observable emits `project(ev)` for every `ev` emitted by the parent observable.
    *
    * @param project Exceptions here are emitted as errors.
    *
    * @see [[mapTo]], [[mapToStrict]], [[mapToUnit]], [[tapEach]]
    */
  def map[B](project: A => B): Self[B]

  /**
    * This observable emits `value` for every event emitted by the parent observable.
    *
    * It is equivalent to `map(_ => value)`, i.e. `value` is re-evaluated on every event.
    * You can use this to sample mutable value, e.g. `mapTo(myInputEl.ref.value)` in Laminar.
    *
    * @param value Exceptions here are emitted as errors.
    *
    * @see [[map]], [[mapToStrict]], [[mapToUnit]]
    */
  def mapTo[B](value: => B): Self[B] = map(_ => value)

  /**
    * Like `mapTo(value)`, except `value` is evaluated by-value (strictly).
    *
    * @see [[mapTo]]
    */
  def mapToStrict[B](value: B): Self[B] = map(_ => value)

  /**
    * Equivalent to `mapTo(())` or `mapToStrict(())`.
    *
    * @see [[mapTo]], [[mapToStrict]]
    */
  @inline def mapToUnit: Self[Unit] = map(_ => ())

  /**
    * Execute a side-effecting callback on every event or update.
    *
    * @param callback Exceptions here are emitted as errors.
    *
    * @note The resulting observable is lazy, so the callback will only be run if it has observers.
    *
    * @see [[Observable.foreach]], [[Observable.addObserver]], [[https://github.com/raquo/Airstream/?tab=readme-ov-file#laziness Docs: Laziness]]
    */
  @inline def tapEach[U](callback: A => U): Self[A] = map { v => callback(v); v }
}
