package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}

class SplittableOptionStream[Input](val stream: EventStream[Option[Input]]) extends AnyVal {

  /** This `.split`-s a Stream of an Option by the Option's `isDefined` property.
    * If you want a different key, use the .split operator directly.
    *
    * @param project - (initialInput, signalOfInput) => output
    *                  `project` is called whenever signal switches from `None` to `Some()`.
    *                  `signalOfInput` starts with `initialInput` value, and updates when
    *                  the parent `signal` updates from `Some(a)` to `Some(b)`.
    * @param ifEmpty - returned if Option is empty, or if the parent `stream` has not emitted
    *                  any events yet. Re-evaluated whenever the parent `stream` switches from
    *                  `Some(a)` to `None`. `ifEmpty` is NOT re-evaluated when the parent
    *                  `stream` emits `None` if the last event it emitted was also a `None`.
    */
  def splitOption[Output](
    project: (Input, Signal[Input]) => Output,
    ifEmpty: => Output
  ): Signal[Output] = {
    new SplittableOptionSignal(stream.startWith(None)).splitOption(project, ifEmpty)
  }

}
