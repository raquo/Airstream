package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}

/** See also: [[EitherStream]] */
class EitherThrowableObservable[A, B, Self[+_] <: Observable[_]](
  private val observable: BaseObservable[Self, Either[Throwable, B]]
) extends AnyVal {

  def throwLeft: Self[B] = {
    observable.map(_.fold(throw _, identity))
  }

}
