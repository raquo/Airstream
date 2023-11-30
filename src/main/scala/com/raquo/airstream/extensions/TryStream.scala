package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}

import scala.util.{Failure, Success, Try}

class TryStream[A](val stream: EventStream[Try[A]]) extends AnyVal {

  /** Emit `x` if parent stream emits `Right(x)`, do nothing otherwise */
  def collectSuccess: EventStream[A] = stream.collect { case Success(ev) => ev }

  /** Emit `pf(x)` if parent stream emits `Success(x)` and `pf` is defined for `x`, do nothing otherwise */
  def collectSuccess[C](pf: PartialFunction[A, C]): EventStream[C] = {
    stream.collectOpt(_.toOption.collect(pf))
  }

  /** Emit `x` if parent stream emits `Left(x)`, do nothing otherwise */
  def collectFailure: EventStream[Throwable] = stream.collect { case Failure(ev) => ev }

  /** Emit `pf(x)` if parent stream emits `Failure(x)` and `pf` is defined for `x`, do nothing otherwise */
  def collectFailure[C](pf: PartialFunction[Throwable, C]): EventStream[C] = {
    stream.collectOpt(_.toEither.left.toOption.collect(pf))
  }

  /** This `.split`-s a stream of Try-s by their `isSuccess` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param success (initialSuccess, signalOfSuccessValues) => output
    *                `success` is called whenever `stream` switches from `Failure()` to `Success()`.
    *                `signalOfSuccessValues` starts with `initialSuccess` value, and updates when
    *                the parent stream updates from `Success(a)` to `Success(b)`.
    * @param failure (initialFailure, signalOfFailureValues) => output
    *                `failure` is called whenever `stream` switches from `Success()` to `Failure()`.
    *                `signalOfFailureValues` starts with `initialFailure` value, and updates when
    *                the parent stream updates from `Failure(a)` to `Failure(b)`.
    */
  def splitTry[B](
    success: (A, Signal[A]) => B,
    failure: (Throwable, Signal[Throwable]) => B
  ): EventStream[B] = {
    new EitherStream(stream.mapToEither).splitEither(failure, success)
  }

}
