package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}

class BooleanObservable[Self[+_] <: Observable[_]](
    val observable: BaseObservable[Self, Boolean]
) extends AnyVal {

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

}
