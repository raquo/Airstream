package com.raquo.airstream

package object web {

  @deprecated("AjaxEventStream renamed to AjaxStream", "15.0.0-M1")
  type AjaxEventStream = AjaxStream

  @deprecated("AjaxEventStream renamed to AjaxStream", "15.0.0-M1")
  lazy val AjaxEventStream: AjaxStream.type = AjaxStream
}
