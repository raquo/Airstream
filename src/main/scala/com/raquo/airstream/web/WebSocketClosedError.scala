package com.raquo.airstream.web

import org.scalajs.dom

/**
  * Wraps a [[dom.CloseEvent websocket closed event]].
  */
final case class WebSocketClosedError(event: dom.CloseEvent) extends Exception
