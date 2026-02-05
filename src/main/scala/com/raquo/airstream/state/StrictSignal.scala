package com.raquo.airstream.state

import com.raquo.airstream.core.{CoreOps, Signal}
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
with DistinctOps[StrictSignal[A], A] {

  @deprecated("Use StrictSignal.map – it behaves like `mapLazy` used to in 17.x", "18.0.0-M3")
  def mapLazy[B](project: A => B): StrictSignal[B] = map(project)

  /** Note: the value of the resulting StrictSignal is evaluated lazily - only on demand. */
  override def map[B](project: A => B): StrictSignal[B] = {
    LazyStrictSignal.mapSignal(
      parentSignal = this,
      project = project,
      parentDisplayName = displayName,
      displayNameSuffix = ".map"
    )
  }

  /** Note: the value of the resulting StrictSignal is evaluated lazily - only on demand. */
  override def distinctTry(isSame: (Try[A], Try[A]) => Boolean): StrictSignal[A] = {
    LazyStrictSignal.distinctSignal(
      parentSignal = this,
      isSame = isSame,
      resetOnStop = false,
      parentDisplayName = displayName,
      displayNameSuffix = ".distinct*"
    )
  }

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
