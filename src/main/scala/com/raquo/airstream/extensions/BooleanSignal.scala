package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.SplittableOneSignal
import com.raquo.airstream.state.StrictSignal

class BooleanSignal(
  private val signal: Signal[Boolean]
) extends AnyVal {

  /**
    * Split a signal of booleans.
    *
    * @param whenTrue  called when the parent signal switches from `false` to `true`.
    *
    *                  The provided signal emits `Unit` on every `true` event from the parent signal.
    *
    * @param whenFalse called when the parent signal switches from `true` to `false`.
    *
    *                  The provided signal emits `Unit` on every `false` event from the parent signal.
    */
  def splitBoolean[C](
    whenTrue: StrictSignal[Unit] => C,
    whenFalse: StrictSignal[Unit] => C
  ): Signal[C] = {
    new SplittableOneSignal(signal).splitOne(identity) { signal =>
      if (signal.now())
        whenTrue(signal.mapToUnit)
      else
        whenFalse(signal.mapToUnit)
    }
  }

}
