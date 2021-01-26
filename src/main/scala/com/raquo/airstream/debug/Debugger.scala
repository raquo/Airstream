package com.raquo.airstream.debug

import scala.util.Try

/** Debugger for observables
  *
  * @param onInitialEval fired when initial value is evaluated. Only for signals.
  */
case class Debugger[-A] (
  topoRank: Int,
  onStart: () => Unit = () => (),
  onStop: () => Unit = () => (),
  onFire: Try[A] => Unit = (_: Try[A]) => (),
  onInitialEval: Try[A] => Unit = (_: Try[A]) => ()
)
