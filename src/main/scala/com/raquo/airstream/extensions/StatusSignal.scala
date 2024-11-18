package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.SplittableOneSignal
import com.raquo.airstream.status.{Pending, Resolved, Status}

class StatusSignal[In, Out](val signal: Signal[Status[In, Out]]) extends AnyVal {

  /** This `.split`-s a signal of Statuses by their type (resolved vs pending).
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param resolved (initialResolved, signalOfResolvedValues) => output
    *                 `resolved` is called whenever parent signal switches from `Pending` to `Resolved`.
    *                 `signalOfResolvedValues` starts with `initialResolved` value, and updates when
    *                 the parent signal emits a new `Resolved` consecutively after another `Resolved`.
    * @param pending  (initialPending, signalOfPendingValues) => output
    *                 `pending` is called whenever parent signal switches from `Resolved` to `Pending`,
    *                 or when the signal's initial value is evaluated (and it's `Pending`, as is typical)
    *                 `signalOfPendingValues` starts with `initialPending` value, and updates when
    *                 the parent signal emits a new `Resolved` consecutively after another `Resolved`.
    *                 This happens when the signal emits inputs faster than the outputs are resolved.
    */
  def splitStatus[A](
    resolved: (Resolved[In, Out], Signal[Resolved[In, Out]]) => A,
    pending: (Pending[In], Signal[Pending[In]]) => A
  ): Signal[A] = {
    new SplittableOneSignal(signal).splitOne(
      key = _.isResolved
    ) { (_, initial, signal) =>
      initial.fold(
        resolved(_, signal.asInstanceOf[Signal[Resolved[In, Out]]]),
        pending(_, signal.asInstanceOf[Signal[Pending[In]]])
      )
    }
  }

}
