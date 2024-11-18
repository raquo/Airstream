package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.SplittableOneSignal

class BooleanSignal(val signal: Signal[Boolean]) extends AnyVal {

  /**
    * Split a signal of booleans.
    *
    * @param whenTrue  called when the parent signal switches from `false` to `true`.
    *                  The provided signal emits `Unit` on every `true` event from the parent signal.
    * @param whenFalse called when the parent signal switches from `true` to `false`.
    *                  The provided signal emits `Unit` on every `false` event from the parent signal.
    */
  def splitBoolean[C](
    whenTrue: Signal[Unit] => C,
    whenFalse: Signal[Unit] => C
  ): Signal[C] = {
    new SplittableOneSignal(signal).splitOne(identity) {
      (_, initial, signal) =>
        if (initial)
          whenTrue(signal.mapToUnit)
        else
          whenFalse(signal.mapToUnit)
    }
  }

}
