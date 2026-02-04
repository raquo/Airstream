package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.StrictSignal

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
    * @param project - signalOfInput => output
    *
    *                  `project` is called whenever the parent signal switches from `None` to `Some(value)`.
    *                  `signalOfInput` starts with an initial `Some(value)`, and updates whenever
    *                  the parent stream updates from `Some(a)` to `Some(b)`.
    *
    *                  You can get the signal's current value with `.now()`.
    *
    * @param ifEmpty - returned if Option is empty, or if the parent stream has not emitted
    *                  any events yet. Re-evaluated whenever the parent `stream` switches from
    *                  `Some(a)` to `None`. `ifEmpty` is NOT re-evaluated when the parent
    *                  stream emits `None` if the last event it emitted was also a `None`.
    */
  def splitOption[B](
    project: StrictSignal[A] => B,
    ifEmpty: => B
  ): Signal[B] = {
    new OptionSignal(stream.startWith(None, cacheInitialValue = true))
      .splitOption(project, ifEmpty)
  }

  def splitOption[B](
    project: StrictSignal[A] => B
  ): Signal[Option[B]] = {
    splitOption(
      signal => Some(project(signal)),
      ifEmpty = None
    )
  }

}
