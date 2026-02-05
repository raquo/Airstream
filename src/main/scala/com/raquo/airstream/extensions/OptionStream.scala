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
}
