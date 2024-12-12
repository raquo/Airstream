package com.raquo.airstream.state

import com.raquo.airstream.core.Signal

import scala.util.Try

/** A Signal that lets you directly query its current value.
  *
  * This means that its current value is kept up to date regardless of observers.
  * How this is actually accomplished is up to the concrete class extending this trait.
  */
trait StrictSignal[+A] extends Signal[A] {

  /** Map StrictSignal to get another StrictSignal, without requiring an Owner.
    *
    * The mechanism is similar to Var.zoomLazy.
    *
    * Just as `zoomLazy`, this method will be renamed in the next major Airstream release.
    */
  def mapLazy[B](project: A => B): StrictSignal[B] = {
    new LazyStrictSignal(
      parentSignal = this,
      zoomIn = project,
      parentDisplayName = displayName,
      displayNameSuffix = ".mapLazy"
    )
  }

  /** Get the signal's current value
    *
    * @throws Throwable the error from the current value's Failure
    */
  override def now(): A = super.now()

  override abstract def tryNow(): Try[A] = super.tryNow()
}
