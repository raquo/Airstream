package com.raquo.airstream.state

import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.{AirstreamError, Transaction}
import com.raquo.airstream.ownership.Owner

import scala.util.{Failure, Success, Try}

/** DerivedVar has the same Var contract as SourceVar, but instead of maintaining its own state
  * it is essentially a lens on the underlying SourceVar.
  *
  * This Var is active for as long as its signal has listeners.
  * Being a StrictSignal, it already starts out with a subscription owned by `owner`,
  * but even if owner kills its subscriptions, this Var's signal might have other listeners.
  */
class DerivedVar[A, B](
  parent: Var[A],
  zoomIn: A => B,
  zoomOut: (A, B) => A,
  owner: Owner
) extends Var[B] {

  override private[state] def underlyingVar: SourceVar[_] = parent.underlyingVar

  private[this] val _varSignal = new DerivedVarSignal(parent, zoomIn, owner)

  // #Note this getCurrentValue implementation is different from SourceVar
  //  - SourceVar's getCurrentValue looks at an internal currentValue variable
  //  - That currentValue gets updated immediately before the signal (in an already existing transaction)
  //  - I hope this doesn't introduce weird transaction related timing glitches
  //  - But even if it does, I think keeping derived var's current value consistent with its signal value
  //    is more important, otherwise it would be madness if the derived var was accessed after its owner
  //    was killed
  override private[state] def getCurrentValue: Try[B] = signal.tryNow()

  override private[state] def setCurrentValue(value: Try[B], transaction: Transaction): Unit = {
    if (_varSignal.isStarted) {
      parent.tryNow() match {
        case Success(parentValue) =>
          // This can update the parent without causing an infinite loop because
          // the parent updates this derived var's signal, it does not call
          // setCurrentValue on this var directly.
          parent.setCurrentValue(value.map(zoomOut(parentValue, _)), transaction)
        case Failure(err) =>
          AirstreamError.sendUnhandledError(VarError(s"Unable to zoom out of derived var when the parent var is failed.", cause = Some(err)))
      }
    } else {
      // #Note We can't just throw here
      //  - The outcome of that would be unpredictable, see comment in Transaction.run
      //  - We also can't put an error into this Var's signal, because no one is listening to it
      //  - The only other thing we could do is send the error into the parent Var, but I'm not sure if that's the right thing to do
      AirstreamError.sendUnhandledError(VarError(s"Unable to set current value ${value} on inactive derived var", cause = None))
    }
  }

  override val signal: StrictSignal[B] = _varSignal
}
