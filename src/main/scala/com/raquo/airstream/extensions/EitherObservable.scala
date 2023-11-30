package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}

/** See also: [[EitherStream]] */
class EitherObservable[A, B, Self[+_] <: Observable[_]](val observable: BaseObservable[Self, Either[A, B]]) extends AnyVal {

  /** Maps the value in Right(x) */
  def mapRight[BB](project: B => BB): Self[Either[A, BB]] = {
    observable.map(_.map(project))
  }

  /** Maps the value in Left(x) */
  def mapLeft[AA](project: A => AA): Self[Either[AA, B]] = {
    observable.map(_.left.map(project))
  }

  /** Maps the values in Left(y) and Right(x) */
  def foldEither[C](
    left: A => C,
    right: B => C
  ): Self[C] = {
    observable.map(_.fold(left, right))
  }

  /** Maps Right(x) to Left(x), and Left(y) to Right(y) */
  def swap: Self[Either[B, A]] = observable.map(_.swap)

  /** Maps Right(x) to Some(x), Left(_) to None */
  def mapToOption: Self[Option[B]] = {
    observable.map(_.toOption)
  }

  /** Maps Right(_) to None, Left(x) to Some(x) */
  def mapLeftToOption: Self[Option[A]] = {
    observable.map(_.left.toOption)
  }

}
