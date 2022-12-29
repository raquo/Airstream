package com.raquo.airstream.state

import com.raquo.airstream.core.{Transaction, WritableSignal}

import scala.util.Try

/** Unlike other signals, this signal's current value is always up to date
  * because a subscription is not needed to maintain it, we just call onTry
  * whenever the Var's current value is updated.
  *
  * Consequently, we expose its current value with now() / tryNow() methods
  * (see StrictSignal).
  */
private[state] class VarSignal[A] private[state](
  initial: Try[A]
) extends WritableSignal[A] with StrictSignal[A] {

  /** SourceVar does not directly depend on other observables, so it breaks the graph. */
  override protected val topoRank: Int = 1

  setCurrentValue(initial, isInitial = true)

  /** Note: we do not check if isStarted() here, this is how we ensure that this
    * signal's current value stays up to date. If this signal is stopped, this
    * value will not be propagated anywhere further though.
    */
  private[state] def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    fireTry(nextValue, transaction)
  }

  override protected def currentValueFromParent(): Try[A] = tryNow() // noop

  override protected def onWillStart(): Unit = () // noop
}
