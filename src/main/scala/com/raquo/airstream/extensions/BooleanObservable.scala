package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.split.Splittable
import com.raquo.airstream.state.StrictSignal

/** See also [[BooleanStream]] for stream-specific operators. */
class BooleanObservable[Self[+_] <: Observable[_]](
  private val observable: BaseObservable[Self, Boolean]
) extends AnyVal {

  def invert: Self[Boolean] = {
    observable.map(!_)
  }

  @inline def not: Self[Boolean] = invert

  def mapTrueToSome[A](value: => A): Self[Option[A]] = {
    observable.map(if (_) Some(value) else None)
  }

  def mapFalseToSome[A](value: => A): Self[Option[A]] = {
    observable.map(if (_) None else Some(value))
  }

  def mapTrueToSeq[M[_], A](seq: => M[A])(implicit splittable: Splittable[M]): Self[M[A]] = {
    observable.map(if (_) seq else splittable.empty)
  }

  def mapFalseToSeq[M[_], A](seq: => M[A])(implicit splittable: Splittable[M]): Self[M[A]] = {
    observable.map(if (_) splittable.empty else seq)
  }

  def foldBoolean[A](
    whenTrue: => A,
    whenFalse: => A
  ): Self[A] = {
    observable map {
      if (_) whenTrue else whenFalse
    }
  }

  /**
    * Split an observable of booleans.
    *
    * @param whenTrue  called when the parent observable switches from `false` to `true`.
    *
    *                  The provided signal emits `Unit` on every `true` event from the parent observable.
    *
    * @param whenFalse called when the parent signal switches from `true` to `false`.
    *
    *                  The provided signal emits `Unit` on every `false` event from the parent observable.
    */
  def splitBoolean[C](
    whenTrue: StrictSignal[Unit] => C,
    whenFalse: StrictSignal[Unit] => C
  ): Self[C] = {
    observable
      .splitOne(identity) { signal =>
        if (signal.now())
          whenTrue(signal.mapToUnit)
        else
          whenFalse(signal.mapToUnit)
      }
  }

}
