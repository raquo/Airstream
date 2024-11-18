package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.split.SplittableOneStream
import com.raquo.airstream.status.{Pending, Resolved, Status}

class StatusStream[In, Out](val stream: EventStream[Status[In, Out]]) extends AnyVal {

  /** Emit `x` if parent stream emits `Resolved(_, x, _)`, do nothing otherwise */
  def collectOutput: EventStream[Out] = stream.collect {
    case Resolved(_, output, _) => output
  }

  /** Emit `pf(x)` if parent stream emits `Resolved(_, x, _)`, do nothing otherwise */
  def collectOutput[Out2](pf: PartialFunction[Out, Out2]): EventStream[Out2] = {
    stream.collectOpt {
      case Resolved(_, output, _) => pf.lift(output)
      case _ => None
    }
  }

  /** Emit `pf(Resolved(...))` if parent stream emits Resolved(...), do nothing otherwise */
  def collectResolved: EventStream[Resolved[In, Out]] = stream.collect {
    case r: Resolved[In @unchecked, Out @unchecked] => r
  }

  /** Emit `pf(Resolved(...))` if parent stream emits Resolved(...) and `pf` is defined for it, do nothing otherwise */
  def collectResolved[Out2](pf: PartialFunction[Resolved[In, Out], Out2]): EventStream[Out2] = {
    stream.collectOpt {
      case r: Resolved[In @unchecked, Out @unchecked] => pf.lift(r)
      case _ => None
    }
  }

  /** Emit `Pending(input)` if parent stream emits that, do nothing otherwise */
  def collectPending: EventStream[Pending[In]] = stream.collect {
    case p: Pending[In @unchecked] => p
  }

  /** Emit `input` if parent stream emits `Pending(input)`, do nothing otherwise */
  def collectPendingInput: EventStream[In] = stream.collect {
    case Pending(input) => input
  }

  /** This `.split`-s a stream of Statuses by their type (resolved vs pending).
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param resolved (initialResolved, signalOfResolvedValues) => output
    *                 `resolved` is called whenever `stream` switches from `Pending` to `Resolved`.
    *                 `signalOfResolvedValues` starts with `initialResolved` value, and updates when
    *                 the parent stream emits a new `Resolved` consecutively after another `Resolved`.
    * @param pending  (initialPending, signalOfPendingValues) => output
    *                 `pending` is called whenever `stream` switches from `Resolved` to `Pending`,
    *                 or when the very first event is emitted (and it's `Pending`, as is typical)
    *                 `signalOfPendingValues` starts with `initialPending` value, and updates when
    *                 the parent stream emits a new `Resolved` consecutively after another `Resolved`.
    *                 This happens when the stream emits inputs faster than the outputs are resolved.
    */
  def splitStatus[A](
    resolved: (Resolved[In, Out], Signal[Resolved[In, Out]]) => A,
    pending: (Pending[In], Signal[Pending[In]]) => A
  ): EventStream[A] = {
    new SplittableOneStream(stream).splitOne(key = _.isResolved) {
      (_, initial, signal) =>
        initial.fold(
          resolved(_, signal.asInstanceOf[Signal[Resolved[In, Out]]]),
          pending(_, signal.asInstanceOf[Signal[Pending[In]]])
        )
    }
  }

}
