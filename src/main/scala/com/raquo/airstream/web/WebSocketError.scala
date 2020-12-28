package com.raquo.airstream.web

final case class WebSocketError[E](event: E) extends Exception
