package com.raquo.airstream.state

import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.{AirstreamError, Transaction}

import scala.util.{Failure, Success, Try}

/** [[LazyDerivedVar]] has the same Var contract as DerivedVar,
  * but it only computes its value lazily, e.g. when you
  * ask for it with .now(), or when its signal has subscribers.
  *
  * Unlike the regular DerivedVar, you don't need to provide an Owner
  * to create LazyDerivedVar, and you're allowed to update this Var
  * even if its signal has no subscribers.
  *
  * @param updateParent (currentParentValue, nextValue) => nextParentValue.
  *                     If `updateParent` returns None, parent will not be updated.
  *                     Use [[LazyDerivedVar.standardErrorsF]] wrapper if you want
  *                     standard error handling.
  */
class LazyDerivedVar[ParentV, ThisV](
  parent: Var[ParentV],
  override val signal: StrictSignal[ThisV],
  updateParent: (Try[ParentV], Try[ThisV]) => Option[Try[ParentV]],
  displayNameSuffix: String
) extends Var[ThisV] {

  override private[state] def underlyingVar: SourceVar[_] = parent.underlyingVar

  // #Note this getCurrentValue implementation is different from SourceVar
  //  - SourceVar's getCurrentValue looks at an internal currentValue variable
  //  - That currentValue gets updated immediately before the signal (in an already existing transaction)
  //  - I hope this doesn't introduce weird transaction related timing glitches
  //  - But even if it does, I think keeping derived var's current value consistent with its signal value
  //    is more important, otherwise it would be madness if the derived var was accessed after its owner
  //    was killed
  override private[state] def getCurrentValue: Try[ThisV] = signal.tryNow()

  override private[state] def setCurrentValue(value: Try[ThisV], transaction: Transaction): Unit = {
    val maybeNextValue = Try(updateParent(parent.tryNow(), value)) match {
      case Success(nextValue) => nextValue
      case Failure(err) => Some(Failure(err))
    }
    maybeNextValue.foreach { nextValue =>
      // This can update the parent without causing an infinite loop because
      // the parent updates this derived var's signal, it does not call
      // setCurrentValue on this var directly.
      parent.setCurrentValue(nextValue, transaction)
    }
  }

  override protected def defaultDisplayName: String = parent.displayName + displayNameSuffix
}

object LazyDerivedVar {

  /** Wrap an `updateParent` callback that only deals with non-error values to handle
    * errors in a standard manner:
    *  - if parent is in error state, leave it as-is, and send unhandled error
    *  - if `updateParent` throws, write the error into parent
    *
    * To use this, wrap your `(ParentV, ThisV) => Option[ParentV]` callback
    * into `standardErrorsF` when instantiating `LazyDerivedVar`.
    */
  def standardErrorsF[ParentV, ThisV](
    updateParent: (ParentV, ThisV) => Option[ParentV]
  )(
    parentTry: Try[ParentV],
    nextTry: Try[ThisV]
  ): Option[Try[ParentV]] =
    parentTry match {
      case Success(parentValue) =>
        nextTry.map(updateParent(parentValue, _)) match {
          case Success(Some(newParentValue)) =>
            Some(Success(newParentValue)) // Update parent var (as per `updateParent`)
          case Success(None) =>
            None // Skip updating parent var (as per `updateParent`)
          case Failure(err) =>
            Some(Failure(err)) // Update parent var with new error
        }
      case Failure(err) =>
        sendUnhandledError(err)
        None // Don't update parent var
    }

  // #TODO[DX] refactor message generation so that we can print the vars
  def sendUnhandledError(err: Throwable): Unit = {
    AirstreamError.sendUnhandledError(
      VarError(s"Unable to zoom out of lazy derived var when the parent var is failed.", cause = Some(err))
    )
  }
}
