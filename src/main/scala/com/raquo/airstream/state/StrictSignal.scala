package com.raquo.airstream.state

import com.raquo.airstream.core.Signal

import scala.util.Try

/** A Signal that lets you directly query its current value.
  *
  * This means that its current value is kept up to date regardless of observers.
  * How this is actually accomplished is up to the concrete class extending this trait.
  */
trait StrictSignal[+A] extends Signal[A] {

  /** Get the signal's current value
    *
    * @throws Throwable the error from the current value's Failure
    */
  override def now(): A = super.now()

  override abstract def tryNow(): Try[A] = super.tryNow()
}
