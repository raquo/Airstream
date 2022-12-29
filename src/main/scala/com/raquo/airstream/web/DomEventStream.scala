package com.raquo.airstream.web

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.custom.{CustomSource, CustomStreamSource}
import org.scalajs.dom

import scala.scalajs.js

object DomEventStream {

  /**
    * This stream, when started, registers an event listener on a specific target
    * like a DOM element, document, or window, and re-emits all events sent to the listener.
    *
    * When this stream is stopped, the listener is removed.
    *
    * @tparam Ev - You need to specify what event type you're expecting.
    *              The event type depends on the event, i.e. eventKey. Look it up on MDN.
    *
    * @param eventTarget any DOM event target, e.g. element, document, or window
    * @param eventKey    DOM event name, e.g. "click", "input", "change"
    * @param useCapture  See section about "useCapture" in https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener
    *
    */
  def apply[Ev <: dom.Event](
    eventTarget: dom.EventTarget,
    eventKey: String,
    useCapture: Boolean = false
  ): EventStream[Ev] = {
    new CustomStreamSource[Ev](
      (fireValue, _, _, _) => {
        // Wrap scala.Function into js.Function only once,
        // ensuring that the same function reference is passed to
        // removeEventListener as was given to addEventListener.
        val eventHandler: js.Function1[Ev, Unit] = fireValue

        CustomSource.Config(
          onStart = () => eventTarget.addEventListener(eventKey, eventHandler, useCapture),
          onStop = () => eventTarget.removeEventListener(eventKey, eventHandler, useCapture)
        )
      }
    )
  }
}
