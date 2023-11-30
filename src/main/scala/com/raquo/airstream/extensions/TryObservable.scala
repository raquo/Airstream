package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}

import scala.util.Try

/** See also: [[OptionStream]] */
class TryObservable[A, Self[+_] <: Observable[_]](val observable: BaseObservable[Self, Try[A]]) extends AnyVal {

  /** Maps the value in Success(x) */
  def mapSuccess[B](project: A => B): Self[Try[B]] = {
    observable.map(_.map(project))
  }

  /** Maps the value in Failure(x) */
  def mapFailure(project: Throwable => Throwable): Self[Try[A]] = {
    observable.map(_.toEither.left.map(project).toTry)
  }

  /** Maps the values in Success(x) and Failure(y) to a common type */
  def foldTry[B](
    failure: Throwable => B,
    success: A => B,
  ): Self[B] = {
    observable.map(_.fold(failure, success))
  }

  /** Convert the Try to Either */
  def mapToEither: Self[Either[Throwable, A]] = {
    observable.map(_.toEither)
  }

  /** Convert the Try to Either and map the value in Left(x) */
  def mapToEither[L](left: Throwable => L): Self[Either[L, A]] = {
    observable.map(_.toEither.left.map(left))
  }

  /** Merge Try[A] into Try[AA >: A] by mapping the value in Failure(y) */
  def recoverFailure[AA >: A](f: Throwable => AA): Self[AA] = {
    observable.map(_.fold(f, identity))
  }

  /** Unwrap Try to "undo" `recoverToTry` – Encode Failure(err) as observable errors, and Success(v) as events */
  def throwFailure: Self[A] = {
    observable.map(_.fold(throw _, identity))
  }

}
