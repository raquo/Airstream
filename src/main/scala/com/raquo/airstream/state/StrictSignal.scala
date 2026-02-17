package com.raquo.airstream.state

import com.raquo.airstream.core.{CoreOps, RecoverOps, ScanLeftSignalOps, Signal}
import com.raquo.airstream.debug.{Debugger, DebugSignalOps}
import com.raquo.airstream.distinct.DistinctOps

import scala.util.Try

/** A Signal that lets you directly query its current value.
  *
  * This means that its current value is kept up to date regardless of observers.
  * How this is actually accomplished is up to the concrete class extending this trait.
  */
trait StrictSignal[+A]
extends Signal[A]
with CoreOps[StrictSignal, A]
with RecoverOps[StrictSignal, A]
with DistinctOps[StrictSignal[A], A]
with ScanLeftSignalOps[StrictSignal, A]
with DebugSignalOps[StrictSignal, A] {

  @deprecated("Use StrictSignal.map – it behaves like `mapLazy` used to in 17.x", "18.0.0-M3")
  def mapLazy[B](project: A => B): StrictSignal[B] = map(project)

  /** Note: the value of the resulting StrictSignal is evaluated lazily - only on demand. */
  override def map[B](project: A => B): StrictSignal[B] = {
    LazyStrictSignal.mapSignal(
      parentSignal = this,
      project = project,
      parentDisplayName = displayName,
      displayNameSuffix = ".map*"
    )
  }

  /** If `recover` is defined and needs to be called, it can do the following:
    *  - Return Some(value) to make this stream emit value
    *  - Return None to make this signal ignore (swallow) this error
    *    - #Warning: Except if this error is the Signal's initial value, then we'll use it anyway.
    *  - Not handle the error (meaning .isDefinedAt(error) must be false) to emit the original error
    *
    * If `recover` throws an exception, it will be wrapped in `ErrorHandlingError` and propagated.
    */
  override protected def mapRecover[B](
    projectValue: A => B,
    recoverError: PartialFunction[Throwable, Option[B]]
  ): StrictSignal[B] = {
    LazyStrictSignal.mapRecoverSignal(
      parentSignal = this,
      projectValue = projectValue,
      recoverError = recoverError,
      parentDisplayName = displayName,
      displayNameSuffix = ".recover*"
    )
  }

  /** Distinct all values (both events and errors) using a comparison function
    *
    * Note: the value of the resulting StrictSignal is evaluated lazily - only on demand.
    *
    * See other distinct* operators in [[DistinctOps]].
    */
  override def distinctTry(isSame: (Try[A], Try[A]) => Boolean): StrictSignal[A] = {
    LazyStrictSignal.distinctSignal(
      parentSignal = this,
      isSame = isSame,
      resetOnStop = false,
      parentDisplayName = displayName,
      displayNameSuffix = ".distinct*"
    )
  }

  override def scanLeftRecover[B](
    makeInitial: Try[A] => Try[B]
  )(
    fn: (Try[B], Try[A]) => Try[B]
  ): StrictSignal[B] = {
    LazyStrictSignal.scanLeftRecoverSignal(
      parentSignal = this,
      makeInitial = makeInitial,
      fn = fn,
      parentDisplayName = displayName,
      displayNameSuffix = ".scanLeft*"
    )
  }

  override def debugWith(debugger: Debugger[A]): StrictSignal[A] =
    LazyStrictSignal.debuggerSignal(
      parentSignal = this,
      debugger = debugger,
      parentDisplayName = displayName,
      displayNameSuffix = ".debug*"
    )

  /** Get the signal's current value
    *
    * @throws Throwable the error from the current value's Failure
    */
  final override def now(): A = super.now()

  // This abstract override makes this method public.
  // super.tryNow() should be the default implementation, but we can't put
  // it here because Signal[+A] does not have a concrete implementation
  // of tryNow, only WritableSignal[A] does.
  // Thus, we put the default implementation in WritableStrictSignal.
  override def tryNow(): Try[A]
}
