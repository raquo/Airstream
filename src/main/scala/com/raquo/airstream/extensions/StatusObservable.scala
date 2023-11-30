package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.status.{Pending, Resolved, Status}

class StatusObservable[In, Out, Self[+_] <: Observable[_]](val observable: BaseObservable[Self, Status[In, Out]]) extends AnyVal {

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

}
