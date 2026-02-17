package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.state.StrictSignal

/** See also: [[EitherStream]] */
class EitherObservable[A, B, Self[+_] <: Observable[_]](
  private val observable: BaseObservable[Self, Either[A, B]]
) extends AnyVal {

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

  def rightOrElse[BB >: B](ifLeft: => BB): Self[BB] = {
    observable.map(_.getOrElse(ifLeft))
  }

  def leftOrElse[AA >: A](ifRight: => AA): Self[AA] = {
    observable.map(_.left.getOrElse(ifRight))
  }

  /** This `.split`-s an Observable of an Either by the Either's `isRight` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param left  signalOfLeftValues => output
    *
    *              `left` is called whenever parent observable switches from `Right()` to `Left()`.
    *              `signalOfLeftValues` starts with an initial left value, and updates when
    *              the parent observable updates from `Left(a)` to `Left(b)`.
    *
    *              You can get the signal's current value with `.now()`.
    *
    * @param right signalOfRightValues => output
    *
    *              `right` is called whenever parent observable switches from `Left()` to `Right()`.
    *              `signalOfRightValues` starts with an initial right value, and updates when
    *              the parent observable updates from `Right(a)` to `Right(b)`.
    *
    *              You can get the signal's current value with `.now()`.
    */
  def splitEither[C](
    left: StrictSignal[A] => C,
    right: StrictSignal[B] => C
  ): Self[C] = {
    // #TODO[Scala3] When we drop Scala 2, use splitMatch macros to implement this.
    observable
      .splitOne(key = _.isRight) { signal =>
        val isRight = signal.key
        if (isRight) {
          right(signal.map(e => e.getOrElse(throw new Exception(s"splitEither: `${signal}` bad right value: $e"))))
        } else {
          left(signal.map(e => e.left.getOrElse(throw new Exception(s"splitEither: `${signal}` bad left value: $e"))))
        }
      }
  }

}
