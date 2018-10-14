package com.raquo.airstream.eventstream

import scala.scalajs.js

// @TODO[Test] Verify throttling

/** [[ThrottleEventStream]] emits the next event only if more than `intervalMillis` ms
  * has elapsed since this stream emitted the previous event.
  *
  * This stream emits an error if the parent stream emits an error (Note: no throttling applied in that case)
  *
  * [[ThrottleEventStream]] acts like a synchronous filter on input events, whereas
  * [[DebounceEventStream]] while also filtering input events, emits the events with a delay.
  */
object ThrottleEventStream {

  def apply[A](parent: EventStream[A], intervalMillis: Int): EventStream[A] = {
    var lastEventTimeMillis: Double = 0

    new FilterEventStream[A](parent, passes = _ => {
      val currentTimeMillis = js.Date.now()
      val timeSinceLastEventMillis = currentTimeMillis - lastEventTimeMillis

      lastEventTimeMillis = currentTimeMillis

      timeSinceLastEventMillis >= intervalMillis
    })
  }
}
