package com.raquo.airstream.state

import com.raquo.airstream.core.{CoreOps, Signal}

import scala.util.Try

/** A Signal that lets you directly query its current value.
  *
  * This means that its current value is kept up to date regardless of observers.
  * How this is actually accomplished is up to the concrete class extending this trait.
  */
trait StrictSignal[+A]
extends Signal[A]
with CoreOps[StrictSignal, A] {

  @deprecated("Use StrictSignal.map – it behaves like `mapLazy` used to in 17.x", "18.0.0-M2")
  def mapLazy[B](project: A => B): StrictSignal[B] = map(project)

  /** Map StrictSignal to get another StrictSignal, without requiring an Owner.
    *
    * The mechanism is similar to Var.zoomLazy.
    *
    * Just as `zoomLazy`, this method will be renamed in the next major Airstream release.
    */
  override def map[B](project: A => B): StrictSignal[B] = {
    new LazyStrictSignal(
      parentSignal = this,
      zoomIn = project,
      parentDisplayName = displayName,
      displayNameSuffix = ".map"
    )
  }

  /** Get the signal's current value
    *
    * @throws Throwable the error from the current value's Failure
    */
  override def now(): A = super.now()

  override def tryNow(): Try[A]
}
