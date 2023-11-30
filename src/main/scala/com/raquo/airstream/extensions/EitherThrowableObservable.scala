package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}

/** See also: [[EitherStream]] */
class EitherThrowableObservable[A, B, Self[+_] <: Observable[_]](val observable: BaseObservable[Self, Either[Throwable, B]]) extends AnyVal {

  def recoverLeft[BB >: B](f: Throwable => BB): Self[BB] = {
    observable.map(_.fold(f, identity))
  }

  def throwLeft: Self[B] = {
    observable.map(_.fold(throw _, identity))
  }

}
