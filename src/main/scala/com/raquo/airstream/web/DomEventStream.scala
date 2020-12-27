package com.raquo.airstream.web

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import org.scalajs.dom

import scala.scalajs.js

/**
  * This stream, when started, registers an event listener on a specific target
  * like a DOM element, document, or window, and re-emits all events sent to the listener.
  *
  * When this stream is stopped, the listener is removed.
  *
  * @param eventTarget any DOM event target, e.g. element, document, or window
  * @param eventKey DOM event name, e.g. "click", "input", "change"
  * @param useCapture See section about "useCapture" in https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener
  *
  */
class DomEventStream[Ev <: dom.Event](
  eventTarget: dom.EventTarget,
  eventKey: String,
  useCapture: Boolean
) extends EventStream[Ev] {

  override protected[airstream] val topoRank: Int = 1

  val eventHandler: js.Function1[Ev, Unit] = { ev =>
    new Transaction(fireValue(ev, _))
  }

  override protected[this] def onStart(): Unit = {
    eventTarget.addEventListener(eventKey, eventHandler, useCapture)
  }

  override protected[this] def onStop(): Unit = {
    eventTarget.removeEventListener(eventKey, eventHandler, useCapture)
  }
}

object DomEventStream {

  def apply[Ev <: dom.Event](
    eventTarget: dom.EventTarget,
    eventKey: String,
    useCapture: Boolean = false
  ): EventStream[Ev] = {
    new DomEventStream[Ev](eventTarget, eventKey, useCapture)
  }
}
