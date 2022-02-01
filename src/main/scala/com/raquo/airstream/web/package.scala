package com.raquo.airstream

package object web {

  @deprecated("AjaxEventStream renamed to AjaxStream", "15.0.0-RC1")
  type AjaxEventStream = AjaxStream

  lazy val AjaxEventStream: AjaxStream.type = AjaxStream
}
