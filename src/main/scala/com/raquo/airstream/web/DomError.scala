package com.raquo.airstream.web

import org.scalajs.dom

/**
  * Wraps a [[dom.Event DOM error event]].
  */
final case class DomError(event: dom.Event) extends Exception
