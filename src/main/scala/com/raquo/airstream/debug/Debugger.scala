package com.raquo.airstream.debug

import scala.util.Try

/** Debugger for observables
  *
  * @param onEvalFromParent Only for signals. Fired when signal calls `currentValueFromParent`, which happens
  *                         1) when the signal is first started and its initial value is evaluated, AND
  *                         2) also when the signal is re-started after being stopped, when that method is called
  *                            to re-sync this signal's value with the parent.
  */
case class Debugger[-A] (
  onStart: () => Unit = () => (),
  onStop: () => Unit = () => (),
  onFire: Try[A] => Unit = (_: Try[A]) => (),
  onEvalFromParent: Try[A] => Unit = (_: Try[A]) => ()
)
