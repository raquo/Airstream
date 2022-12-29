package com.raquo.airstream.debug

import scala.util.Try

/** Debugger for observables
  *
  * @param onEvalFromParent Only for signals. Fired when signal calls `currentValueFromParent`, which happens
  *                         1) when the signal is first started and its initial value is evaluated, AND
  *                         2) also when the signal is re-started after being stopped, when that method is called
  *                            to re-sync this signal's value with the parent.
  *                         #TODO[Integrity]: I'm not 100% sure if this hook reports correctly.
  *                          - It actually reports the debugger signal's own calls to currentValueFromParent
  *                            and I'm not 100% sure that they match with the parent's calls. I think they
  *                            should match, at least if parent signal is always used via its debug signal,
  *                            but to be honest I'm not really sure.
  */
case class Debugger[-A] (
  onStart: () => Unit = () => (),
  onStop: () => Unit = () => (),
  onFire: Try[A] => Unit = (_: Try[A]) => (),
  onEvalFromParent: Try[A] => Unit = (_: Try[A]) => ()
)
