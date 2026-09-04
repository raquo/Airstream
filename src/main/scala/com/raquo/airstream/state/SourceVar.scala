package com.raquo.airstream.state

import com.raquo.airstream.core.Transaction

import scala.util.Try

/** The regular Var that's created with `Var.apply`.
  *
  * See also DerivedVar, created with `myVar.zoom(a => b)((a, b) => a)(owner)`,
  * and LazyDerivedVar, created with `myVar.zoomLazy(a => b)((a, b) => a)`
  */
class SourceVar[A](initial: Try[A]) extends Var[A] {

  private var currentValue: Try[A] = initial

  /** VarSignal is a private type, do not expose it */
  private val _varSignal = new VarSignal[A](
    initial = currentValue,
    parentDisplayName = displayName
  )

  override private[state] def underlyingVar: SourceVar[?] = this

  override private[state] def getCurrentValue: Try[A] = currentValue

  override private[state] def setCurrentValue(
    value: Try[A],
    transaction: Transaction
  ): Unit = {
    currentValue = value
    _varSignal.onTry(value, transaction)
  }

  override val signal: StrictSignal[A] = _varSignal
}
