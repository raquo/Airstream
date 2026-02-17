package com.raquo.airstream.core

import scala.util.{Failure, Success, Try}

/** Operators related to bringing values from the error channel into the main channel */
trait RecoverOps[+Self[+_], +A] {

  // @TODO[API] I don't like the Option[O] output type here very much. We should consider a sentinel error object instead (need to check performance). Or maybe add a recoverOrSkip method or something?
  /** @param pf Note: guarded against exceptions */
  def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Self[B] = {
    mapRecover(identity, pf)
  }

  def recoverIgnoreErrors: Self[A] = {
    recover[A](_ => None)
  }

  /** Convert this to an observable that emits Failure(err) instead of erroring */
  def recoverToTry: Self[Try[A]] = {
    mapRecover(Success(_), err => Some(Failure(err)))
  }

  /** Convert this to an observable that emits Left(err) instead of erroring */
  def recoverToEither: Self[Either[Throwable, A]] = {
    mapRecover(Right(_), err => Some(Left(err)))
  }

  // #TODO[Integrity,API] this offers filtering on the error channel, but Signals can't safely do filtering.
  //  - We had to introduce `InitialValueError` to handle that edge case for now as of 18.0.0-M3.
  //  - Corollary: Signals can't do `recoverIgnoreErrors` without becoming a PartialSignal or providing a fallbackInitialValue
  /** If `recover` is defined and needs to be called, it can do the following:
   *  - Return Some(value) to make this observable emit `value`
   *  - Return None to make this observable ignore (swallow) this error
   *    - #Warning: we're unable to swallow a Signal's initial value
   *  - Not handle the error (meaning .isDefinedAt(error) must be false) to emit the original error
   *
   * If `recover` throws an exception, it will be wrapped in `ErrorHandlingError` and propagated.
   */
  protected def mapRecover[B](
    projectValue: A => B,
    recoverError: PartialFunction[Throwable, Option[B]]
  ): Self[B]

}
