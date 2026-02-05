package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.SplittableOneSignal
import com.raquo.airstream.state.StrictSignal
import com.raquo.airstream.status.{Pending, Resolved, Status}

class StatusSignal[In, Out](
  private val signal: Signal[Status[In, Out]]
) extends AnyVal {

  /** This `.split`-s a signal of Statuses by their type (resolved vs pending).
    * If you want a different key, use the .splitOne operator directly.
    *
    * @param resolved signalOfResolvedValues => output
    *
    *                 `resolved` is called whenever parent signal switches from `Pending` to `Resolved`.
    *                 `signalOfResolvedValues` starts with an initial `Resolved` value, and updates when
    *                 the parent signal emits a new `Resolved` consecutively after another `Resolved`.
    *
    *                 You can get the signal's current value with `.now()`.
    *
    * @param pending  signalOfPendingValues => output
    *
    *                 `pending` is called whenever parent signal switches from `Resolved` to `Pending`,
    *                 or when the signal's initial value is evaluated (and it's `Pending`, as is typical)
    *                 `signalOfPendingValues` starts with an initial `Pending` value, and updates when
    *                 the parent signal emits a new `Pending` consecutively after another `Pending`.
    *                 This happens when the signal emits inputs faster than the outputs are resolved.
    *
    *                 You can get the signal's current value with `.now()`.
    */
  def splitStatus[A](
    resolved: StrictSignal[Resolved[In, Out]] => A,
    pending: StrictSignal[Pending[In]] => A
  ): Signal[A] = {
    new SplittableOneSignal(signal).splitOne(
      key = _.isResolved
    ) { signal =>
      signal.now().fold(
        resolved = _ => resolved(signal.asInstanceOf[StrictSignal[Resolved[In, Out]]]),
        pending = _ => pending(signal.asInstanceOf[StrictSignal[Pending[In]]])
      )
    }
  }

}
