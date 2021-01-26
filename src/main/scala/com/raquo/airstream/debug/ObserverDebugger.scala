package com.raquo.airstream.debug

import scala.util.Try

/** @param sourceName printed as prefix when logging
  */
case class ObserverDebugger[-A](
  sourceName: String,
  onFire: Try[A] => Unit = (_: Try[A]) => ()
)
