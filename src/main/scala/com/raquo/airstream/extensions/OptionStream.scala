package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}

/** See also: [[OptionObservable]] */
class OptionStream[A](val stream: EventStream[Option[A]]) extends AnyVal {

  /** Emit `x` if parent stream emits `Some(x)`, do nothing otherwise */
  def collectSome: EventStream[A] = stream.collect { case Some(ev) => ev }

  /** Emit `pf(x)` if parent stream emits `Some(x)` and `pf` is defined for `x`, do nothing otherwise */
  def collectSome[B](pf: PartialFunction[A, B]): EventStream[B] = {
    stream.collectOpt(_.collect(pf))
  }

  /** This `.split`-s a Stream of an Option by the Option's `isDefined` property.
    * If you want a different key, use the .split operator directly.
    *
    * @param project - (initialInput, signalOfInput) => output
    *                  `project` is called whenever the parent signal switches from `None` to `Some()`.
    *                  `signalOfInput` starts with `initialInput` value, and updates when
    *                  the parent stream updates from `Some(a)` to `Some(b)`.
    * @param ifEmpty - returned if Option is empty, or if the parent stream has not emitted
    *                  any events yet. Re-evaluated whenever the parent `stream` switches from
    *                  `Some(a)` to `None`. `ifEmpty` is NOT re-evaluated when the parent
    *                  stream emits `None` if the last event it emitted was also a `None`.
    */
  def splitOption[B](
    project: (A, Signal[A]) => B,
    ifEmpty: => B
  ): Signal[B] = {
    new OptionSignal(stream.startWith(None, cacheInitialValue = true)).splitOption(project, ifEmpty)
  }

  def splitOption[B](
    project: (A, Signal[A]) => B
  ): Signal[Option[B]] = {
    splitOption(
      (initial, someSignal) => Some(project(initial, someSignal)),
      ifEmpty = None
    )
  }

}
