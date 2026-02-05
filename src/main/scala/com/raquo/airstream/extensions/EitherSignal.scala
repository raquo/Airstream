package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.SplittableOneSignal
import com.raquo.airstream.state.StrictSignal

class EitherSignal[A, B](val signal: Signal[Either[A, B]]) extends AnyVal {

  /** This `.split`-s a Signal of an Either by the Either's `isRight` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param left  signalOfLeftValues => output
    *
    *              `left` is called whenever parent signal switches from `Right()` to `Left()`.
    *              `signalOfLeftValues` starts with an initial left value, and updates when
    *              the parent signal updates from `Left(a)` to `Left(b)`.
    *
    *              You can get the signal's current value with `.now()`.
    *
    * @param right signalOfRightValues => output
    *
    *              `right` is called whenever parent signal switches from `Left()` to `Right()`.
    *              `signalOfRightValues` starts with an initial right value, and updates when
    *              the parent signal updates from `Right(a)` to `Right(b)`.
    *
    *              You can get the signal's current value with `.now()`.
    */
  def splitEither[C](
    left: StrictSignal[A] => C,
    right: StrictSignal[B] => C
  ): Signal[C] = {
    // #TODO[Scala3] When we drop Scala 2, use splitMatch macros to implement this.
    new SplittableOneSignal(signal).splitOne(key = _.isRight) { signal =>
      val isRight = signal.key
      if (isRight) {
        right(signal.map(e => e.getOrElse(throw new Exception(s"splitEither: `${signal}` bad right value: $e"))))
      } else {
        left(signal.map(e => e.left.getOrElse(throw new Exception(s"splitEither: `${signal}` bad left value: $e"))))
      }
    }
  }

}
