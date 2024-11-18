package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.split.SplittableOneStream

class BooleanStream(val stream: EventStream[Boolean]) extends AnyVal {

  /**
    * Split a stream of booleans.
    *
    * @param trueF  called when the parent stream switches from `false` to `true`.
    *               The provided signal emits `Unit` on every `true` event emitted by the stream.
    * @param falseF called when the parent stream switches from `true` to `false`.
    *               The provided signal emits `Unit` on every `false` event emitted by the stream.
    */
  def splitBoolean[C](
    trueF: Signal[Unit] => C,
    falseF: Signal[Unit] => C
  ): EventStream[C] = {
    new SplittableOneStream(stream).splitOne(identity) {
      (_, initial, signal) =>
        if (initial)
          trueF(signal.mapToUnit)
        else
          falseF(signal.mapToUnit)
    }
  }
}
