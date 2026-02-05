package com.raquo.airstream.state

import com.raquo.airstream.core.WritableSignal

import scala.util.Try

/** This trait needs to exist to make this override technically possible.
  * See the comment on [[StrictSignal.tryNow]].
  */
trait WritableStrictSignal[A]
extends StrictSignal[A]
with WritableSignal[A] {

  override def tryNow(): Try[A] = super.tryNow()
}
