package com.raquo.airstream.state

import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.{AirstreamError, Transaction}
import com.raquo.airstream.ownership.Owner

import scala.util.{Failure, Success, Try}

// #nc Update comments

/** DerivedVar has the same Var contract as SourceVar, but instead of maintaining its own state
  * it is essentially a lens on the underlying SourceVar.
  *
  * This Var is active for as long as its signal has listeners.
  * Being a StrictSignal, it already starts out with a subscription owned by `owner`,
  * but even if owner kills its subscriptions, this Var's signal might have other listeners.
  */
class PureDerivedVar[A, B](
  parent: Var[A],
  zoomIn: A => B,
  zoomOut: (A, B) => A,
  displayNameSuffix: String
) extends Var[B] {

  override private[state] def underlyingVar: SourceVar[_] = parent.underlyingVar

  private[this] val _varSignal = new PureDerivedVarSignal(parent, zoomIn, displayName)

  // #Note this getCurrentValue implementation is different from SourceVar
  //  - SourceVar's getCurrentValue looks at an internal currentValue variable
  //  - That currentValue gets updated immediately before the signal (in an already existing transaction)
  //  - I hope this doesn't introduce weird transaction related timing glitches
  //  - But even if it does, I think keeping derived var's current value consistent with its signal value
  //    is more important, otherwise it would be madness if the derived var was accessed after its owner
  //    was killed
  override private[state] def getCurrentValue: Try[B] = signal.tryNow()

  override private[state] def setCurrentValue(value: Try[B], transaction: Transaction): Unit = {
    // #nc Unlike the old DerivedVar, we don't check `_varSignal.isStarted` before updating the parent.
    //  - Is that "natural" because we don't have an explicit "owner" here, or is that a change in semantics?
    parent.tryNow() match {
      case Success(parentValue) =>
        // This can update the parent without causing an infinite loop because
        // the parent updates this derived var's signal, it does not call
        // setCurrentValue on this var directly.
        val nextValue = value.map(zoomOut(parentValue, _))
        // println(s">> parent.setCurrentValue($nextValue)")
        parent.setCurrentValue(nextValue, transaction)

      case Failure(err) =>
        AirstreamError.sendUnhandledError(VarError(s"Unable to zoom out of derived var when the parent var is failed.", cause = Some(err)))
    }
  }

  override val signal: StrictSignal[B] = _varSignal

  override protected def defaultDisplayName: String = parent.displayName + displayNameSuffix
}
