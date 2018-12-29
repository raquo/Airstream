package com.raquo.airstream.signal

import scala.util.Try

/** A Signal that lets you directly query its current value.
  *
  * This means that its current value is kept up to date regardless of observers.
  * How this is actually accomplished is up to the concrete class extending this trait.
  */
trait StrictSignal[+A] extends Signal[A] {

  override def now(): A = super.now()

  override def tryNow(): Try[A] = super.tryNow()
}
