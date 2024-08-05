package com.raquo.airstream.state

import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.{AirstreamError, Transaction}

import scala.util.{Failure, Success, Try}

/** LazyDerivedVar has the same Var contract as DerivedVar, but it only computes
  * its value lazily, e.g. when you ask for it with .now(), or when its signal
  * has subscribers.
  *
  * Unlike the regular DerivedVar, you don't need to provide an Owner to create
  * LazyDerivedVar, and you're allowed to update this Var even if its signal has
  * no subscribers.
  *
  * @param zoomOut
  *   (currentParentValue, nextValue) => nextParentValue.
  */
class LazyDerivedVar[A, B](
    parent: Var[A],
    override val signal: StrictSignal[B],
    zoomOut: (A, B) => A,
    displayNameSuffix: String
) extends Var[B] {

  override private[state] def underlyingVar: SourceVar[_] = parent.underlyingVar

  // #Note this getCurrentValue implementation is different from SourceVar
  //  - SourceVar's getCurrentValue looks at an internal currentValue variable
  //  - That currentValue gets updated immediately before the signal (in an already existing transaction)
  //  - I hope this doesn't introduce weird transaction related timing glitches
  //  - But even if it does, I think keeping derived var's current value consistent with its signal value
  //    is more important, otherwise it would be madness if the derived var was accessed after its owner
  //    was killed
  override private[state] def getCurrentValue: Try[B] = signal.tryNow()

  override private[state] def setCurrentValue(
      value: Try[B],
      transaction: Transaction
  ): Unit = {
    parent.signal.tryNow() match {
      case Success(parentValue) =>
        // This can update the parent without causing an infinite loop because
        // the parent updates this derived var's signal, it does not call
        // setCurrentValue on this var directly.
        val nextValue = value.map(zoomOut(parentValue, _))
        parent.setCurrentValue(nextValue, transaction)

      case Failure(err) =>
        AirstreamError.sendUnhandledError(
          VarError(
            s"Unable to zoom out of lazy derived var when the parent var is failed.",
            cause = Some(err)
          )
        )
    }
  }

  override protected def defaultDisplayName: String =
    parent.displayName + displayNameSuffix
}
