package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal

import scala.util.Try

class TrySignal[A](val signal: Signal[Try[A]]) extends AnyVal {

  /** This `.split`-s a signal of Try-s by their `isSuccess` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param success (initialSuccess, signalOfSuccessValues) => output
    *                `success` is called whenever the parent signal switches from `Failure()` to `Success()`.
    *                `signalOfSuccessValues` starts with `initialSuccess` value, and updates when
    *                the parent signal updates from `Success(a)` to `Success(b)`.
    * @param failure (initialFailure, signalOfFailureValues) => output
    *                `failure` is called whenever the parent stream switches from `Success()` to `Failure()`.
    *                `signalOfFailureValues` starts with `initialFailure` value, and updates when
    *                the parent signal updates from `Failure(a)` to `Failure(b)`.
    */
  def splitTry[B](
    success: (A, Signal[A]) => B,
    failure: (Throwable, Signal[Throwable]) => B
  ): Signal[B] = {
    new EitherSignal(signal.mapToEither).splitEither(failure, success)
  }

}
