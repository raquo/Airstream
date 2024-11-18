package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.SplittableOneSignal

class EitherSignal[A, B](val signal: Signal[Either[A, B]]) extends AnyVal {

  /** This `.split`-s a Signal of an Either by the Either's `isRight` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param left  (initialLeft, signalOfLeftValues) => output
    *              `left` is called whenever parent signal switches from `Right()` to `Left()`.
    *              `signalOfLeftValues` starts with `initialLeft` value, and updates when
    *              the parent signal updates from `Left(a)` to `Left(b)`.
    * @param right (initialRight, signalOfRightValues) => output
    *              `right` is called whenever parent signal switches from `Left()` to `Right()`.
    *              `signalOfRightValues` starts with `initialRight` value, and updates when
    *              the parent signal updates from `Right(a) to `Right(b)`.
    */
  def splitEither[C](
    left: (A, Signal[A]) => C,
    right: (B, Signal[B]) => C
  ): Signal[C] = {
    new SplittableOneSignal(signal).splitOne(key = _.isRight) {
      (_, initial, signal) =>
        initial match {
          case Right(v) =>
            right(v, signal.map(e => e.getOrElse(throw new Exception(s"splitEither: `${signal}` bad right value: $e"))))
          case Left(v) =>
            left(v, signal.map(e => e.left.getOrElse(throw new Exception(s"splitEither: `${signal}` bad left value: $e"))))
        }
    }
  }

}
