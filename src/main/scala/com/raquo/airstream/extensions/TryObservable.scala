package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.state.StrictSignal

import scala.util.Try

/** See also [[TryStream]] for stream-specific try operators */
class TryObservable[A, Self[+_] <: Observable[_]](
  private val observable: BaseObservable[Self, Try[A]]
) extends AnyVal {

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

  /** Merge Try[A] into AA >: A by mapping the value in Failure(y) */
  def successOrElse[AA >: A](f: Throwable => AA): Self[AA] = {
    observable.map(_.fold(f, identity))
  }

  /** Unwrap Try to "undo" `recoverToTry` – Encode Failure(err) as observable errors, and Success(v) as events */
  def throwFailure: Self[A] = {
    observable.map(_.fold(throw _, identity))
  }

  /** This `.split`-s an observable of Try-s by their `isSuccess` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param success signalOfSuccessValues => output
    *
    *                `success` is called whenever the parent observable switches from `Failure()` to `Success()`.
    *                `signalOfSuccessValues` starts with an initial `Success` value, and updates when
    *                the parent observable updates from `Success(a)` to `Success(b)`.
    *
    *                You can get the signal's current value with `.now()`.
    *
    * @param failure signalOfFailureValues => output
    *
    *                `failure` is called whenever the parent observable switches from `Success()` to `Failure()`.
    *                `signalOfFailureValues` starts with an initial `Failure` value, and updates when
    *                the parent observable updates from `Failure(a)` to `Failure(b)`.
    *
    *                You can get the signal's current value with `.now()`.
    */
  def splitTry[B](
    success: StrictSignal[A] => B,
    failure: StrictSignal[Throwable] => B
  ): Self[B] = {
    // #TODO[Scala3] When we drop Scala 2, use splitMatch macros to implement this.
    observable
      .splitOne(key = _.isSuccess) { signal =>
        val isRight = signal.key
        if (isRight) {
          success(signal.map(e => e.getOrElse(throw new Exception(s"splitTry: `${signal}` bad success value: $e"))))
        } else {
          failure(signal.map(e => e.failed.getOrElse(throw new Exception(s"splitTry: `${signal}` bad failure value: $e"))))
        }
      }
  }
}
