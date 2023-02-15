package com.raquo.airstream.split

import com.raquo.airstream.core.Signal

class SplittableOptionSignal[Input](val signal: Signal[Option[Input]]) extends AnyVal {

  /** This `.split`-s a Signal of an Option by the Option's `isDefined` property.
    * If you want a different key, use the .split operator directly.
    *
    * @param project - (initialInput, signalOfInput) => output
    *                  `project` is called whenever signal switches from `None` to `Some()`.
    *                  `signalOfInput` starts with `initialInput` value, and updates when
    *                  the parent `signal` updates from `Some(a)` to `Some(b)`.
    * @param ifEmpty - returned if Option is empty. Evaluated whenever the parent
    *                  `signal` switches from `Some(a)` to `None`, or when the parent signal
    *                  starts with a `None`. `ifEmpty` is NOT re-evaluated when the
    *                  parent signal emits `None` if its value is already `None`.
    */
  def splitOption[Output](
    project: (Input, Signal[Input]) => Output,
    ifEmpty: => Output
  ): Signal[Output] = {
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    signal
      .distinctByFn((prev, next) => prev.isEmpty && next.isEmpty) // Ignore consecutive `None` events
      .split(
        key = _ => (),
        duplicateKeys = DuplicateKeysConfig.noWarnings
      )(
        (_, initial, signal) => project(initial, signal)
      )
      .map(_.getOrElse(ifEmpty))
  }

}
