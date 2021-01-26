package com.raquo.airstream.debug

import scala.util.Try

/** @param sourceName printed as prefix when logging
  * @param onInitialEval when initial value is evaluated. Only for signals.
  */
case class ObservableDebugger[-A] (
  topoRank: Int,
  onStart: () => Unit = () => (),
  onStop: () => Unit = () => (),
  onFire: Try[A] => Unit = (_: Try[A]) => (),
  onInitialEval: Try[A] => Unit = (_: Try[A]) => ()
)
