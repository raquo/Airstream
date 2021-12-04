package com.raquo.airstream.state

import com.raquo.airstream.core.Transaction

import scala.util.Try

/** The regular Var that's created with `Var.apply`.
  *
  * See also DerivedVar, created with `myVar.zoom(a => b)(b => a)(owner)`
  */
class SourceVar[A] private[state](initial: Try[A]) extends Var[A] {

  private[this] var currentValue: Try[A] = initial

  /** VarSignal is a private type, do not expose it */
  private[this] val _varSignal = new VarSignal[A](initial = currentValue)

  override private[state] def underlyingVar: SourceVar[_] = this

  override private[state] def getCurrentValue: Try[A] = currentValue

  override private[state] def setCurrentValue(value: Try[A], transaction: Transaction): Unit = {
    currentValue = value
    _varSignal.onTry(value, transaction)
  }

  override val signal: StrictSignal[A] = _varSignal
}
