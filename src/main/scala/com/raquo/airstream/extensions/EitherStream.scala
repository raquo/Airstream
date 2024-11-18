package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.split.SplittableOneStream

/** See also: [[EitherObservable]] */
class EitherStream[A, B](val stream: EventStream[Either[A, B]]) extends AnyVal {

  /** Emit `x` if parent stream emits `Left(x)`, do nothing otherwise */
  def collectLeft: EventStream[A] = stream.collect { case Left(ev) => ev }

  /** Emit `pf(x)` if parent stream emits `Left(x)` and `pf` is defined for `x`, do nothing otherwise */
  def collectLeft[C](pf: PartialFunction[A, C]): EventStream[C] = {
    stream.collectOpt(_.left.toOption.collect(pf))
  }

  /** Emit `x` if parent stream emits `Right(x)`, do nothing otherwise */
  def collectRight: EventStream[B] = stream.collect { case Right(ev) => ev }

  /** Emit `pf(x)` if parent stream emits `Right(x)` and `pf` is defined for `x`, do nothing otherwise */
  def collectRight[C](pf: PartialFunction[B, C]): EventStream[C] = {
    stream.collectOpt(_.toOption.collect(pf))
  }

  /** This `.split`-s a stream of Either-s by their `isRight` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param left  (initialLeft, signalOfLeftValues) => output
    *              `left` is called whenever `stream` switches from `Right()` to `Left()`.
    *              `signalOfLeftValues` starts with `initialLeft` value, and updates when
    *              the parent stream updates from `Left(a)` to Left(b)`.
    * @param right (initialRight, signalOfRightValues) => output
    *              `right` is called whenever `stream` switches from `Left()` to `Right()`.
    *              `signalOfRightValues` starts with `initialRight` value, and updates when
    *              the parent stream updates from `Right(a)` to `Right(b)`.
    */
  def splitEither[C](
    left: (A, Signal[A]) => C,
    right: (B, Signal[B]) => C
  ): EventStream[C] = {
    new SplittableOneStream(stream).splitOne(key = _.isRight) {
      (_, initial, signal) =>
        initial match {
          case Right(v) =>
            right(v, signal.map(e => e.getOrElse(throw new Exception(s"splitEither: `${stream}` bad right value: $e"))))
          case Left(v) =>
            left(v, signal.map(e => e.left.getOrElse(throw new Exception(s"splitEither: `${stream}` bad left value: $e"))))
        }
    }
  }

}
