package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}

class BooleanObservable[Self[+_] <: Observable[_]](val observable: BaseObservable[Self, Boolean]) extends AnyVal {

  def invert: Self[Boolean] = {
    observable.map(!_)
  }

  @inline def not: Self[Boolean] = invert

  def foldBoolean[A](
    whenTrue: => A,
    whenFalse: => A
  ): Self[A] = {
    observable map {
      if (_) whenTrue else whenFalse
    }
  }

  /**
    * Map the observable to an Option.
    * @param a the value to map to when the observable is true
    */
  def mapTrueToSome[A](a: => A): Self[Option[A]] = observable.map {
    case true => Some(a)
    case false => None
  }

  /**
    * Map the observable to an Option.
    * @param a the value to map to when the observable is false
    */
  def mapFalseToSome[A](a: => A): Self[Option[A]] = observable.map {
    case true => None
    case false => Some(a)
  }

}
