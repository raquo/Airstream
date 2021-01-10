package com.raquo.airstream.vars

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.signal.StrictSignal

import scala.util.Try

/** The regular Var that's created with `Var.apply`.
  *
  * See also DerivedVar, created with `myVar.zoom(a => b)(b => a)(owner)`
  */
class SourceVar[A] private[vars](initial: Try[A]) extends Var[A] {

  private[this] var currentValue: Try[A] = initial

  /** VarSignal is a private type, do not expose it */
  private[this] val _varSignal = new VarSignal[A](initialValue = currentValue)

  override private[vars] def underlyingVar: SourceVar[_] = this

  override private[vars] def getCurrentValue: Try[A] = currentValue

  override private[vars] def setCurrentValue(value: Try[A], transaction: Transaction): Unit = {
    currentValue = value
    _varSignal.onTry(value, transaction)
  }

  override val signal: StrictSignal[A] = _varSignal
}
