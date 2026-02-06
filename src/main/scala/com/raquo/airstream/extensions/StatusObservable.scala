package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.state.StrictSignal
import com.raquo.airstream.status.{Pending, Resolved, Status}

/** See also [[StatusStream]] for stream-specific status operators */
class StatusObservable[In, Out, Self[+_] <: Observable[_]](
  private val observable: BaseObservable[Self, Status[In, Out]]
) extends AnyVal {

  /** Map the output value in Resolved */
  def mapOutput[Out2](project: Out => Out2): Self[Status[In, Out2]] = {
    observable.map(_.mapOutput(project))
  }

  /** Map the input value in both Pending and Resolved */
  def mapInput[In2](project: In => In2): Self[Status[In2, Out]] = {
    observable.map(_.mapInput(project))
  }

  /** Map Resolved(...) to Right(project(Resolved(...))), and Pending(y) to Left(Pending(y)) */
  def mapResolved[B](
    project: Resolved[In, Out] => B
  ): Self[Either[Pending[In], B]] = {
    foldStatus(r => Right(project(r)), Left(_))
  }

  /** Map Pending(x) to Left(project(Pending(x))), and Resolved(...) to Right(Resolved(...)) */
  def mapPending[B](
    project: Pending[In] => B
  ): Self[Either[B, Resolved[In, Out]]] = {
    foldStatus(Right(_), p => Left(project(p)))
  }

  /** Map Resolved(...) with `resolved`, and Pending(x) with `pending`, to a common type */
  def foldStatus[B](
    resolved: Resolved[In, Out] => B,
    pending: Pending[In] => B
  ): Self[B] = {
    observable.map(_.fold(resolved, pending))
  }

  /** This `.split`-s an observable of Statuses by their type (resolved vs pending).
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param resolved signalOfResolvedValues => output
    *
    *                 `resolved` is called whenever parent observable switches from `Pending` to `Resolved`.
    *                 `signalOfResolvedValues` starts with an initial `Resolved` value, and updates when
    *                 the parent observable emits a new `Resolved` consecutively after another `Resolved`.
    *
    *                 You can get the signal's current value with `.now()`.
    *
    * @param pending  signalOfPendingValues => output
    *
    *                 `pending` is called whenever parent observable switches from `Resolved` to `Pending`,
    *                 or when the signal's initial value is evaluated (and it's `Pending`, as is typical)
    *                 `signalOfPendingValues` starts with an initial `Pending` value, and updates when
    *                 the parent observable emits a new `Pending` consecutively after another `Pending`.
    *                 This happens when the observable emits inputs faster than the outputs are resolved.
    *
    *                 You can get the signal's current value with `.now()`.
    */
  def splitStatus[A](
    resolved: StrictSignal[Resolved[In, Out]] => A,
    pending: StrictSignal[Pending[In]] => A
  ): Self[A] = {
    // #TODO[Scala3] When we drop Scala 2, use splitMatch macros to implement this.
    observable
      .splitOne(
        key = _.isResolved
      ) { signal =>
        signal.now().fold(
          resolved = _ => resolved(signal.asInstanceOf[StrictSignal[Resolved[In, Out]]]),
          pending = _ => pending(signal.asInstanceOf[StrictSignal[Pending[In]]])
        )
      }
  }

}
