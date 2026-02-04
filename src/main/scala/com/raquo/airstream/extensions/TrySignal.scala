package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.StrictSignal

import scala.util.Try

class TrySignal[A](val signal: Signal[Try[A]]) extends AnyVal {

  /** This `.split`-s a signal of Try-s by their `isSuccess` property.
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param success signalOfSuccessValues => output
    *
    *                `success` is called whenever the parent signal switches from `Failure()` to `Success()`.
    *                `signalOfSuccessValues` starts with an initial `Success` value, and updates when
    *                the parent signal updates from `Success(a)` to `Success(b)`.
    *
    *                You can get the signal's current value with `.now()`.
    *
    * @param failure signalOfFailureValues => output
    *
    *                `failure` is called whenever the parent stream switches from `Success()` to `Failure()`.
    *                `signalOfFailureValues` starts with an initial `Failure` value, and updates when
    *                the parent signal updates from `Failure(a)` to `Failure(b)`.
    *
    *                You can get the signal's current value with `.now()`.
    */
  def splitTry[B](
    success: StrictSignal[A] => B,
    failure: StrictSignal[Throwable] => B
  ): Signal[B] = {
    new EitherSignal(signal.mapToEither).splitEither(failure, success)
  }

}
