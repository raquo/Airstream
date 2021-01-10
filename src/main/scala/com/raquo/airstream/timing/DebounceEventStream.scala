package com.raquo.airstream.timing

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{EventStream, Transaction}

import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.util.Try

// @TODO[Test] Verify debounce

/** [[DebounceEventStream]] emits the last event emitted by `parent`, but only after `delayFromLastEventMillis` ms
  * has elapsed since `parent` emitted the previous event. So essentially, this stream emits the parent's last event
  * once the parent stops emitting events for a while.
  *
  * [[DebounceEventStream]] is an async filter, emitting some of the input events, and with a delay, whereas
  * [[ThrottleEventStream]] acts like a synchronous filter on input events
  */
class DebounceEventStream[A](
  override protected[this] val parent: EventStream[A],
  delayFromLastEventMs: Int
) extends EventStream[A] with SingleParentObservable[A, A] with InternalTryObserver[A] {

  private[this] var maybeLastTimeoutHandle: js.UndefOr[SetTimeoutHandle] = js.undefined

  /** All emitted events are delayed, so we fire up a new transaction and reset topoRank */
  override protected[airstream] val topoRank: Int = 1

  /** Every time [[parent]] emits an event, we clear the previous timer and set a new one.
    * This stream only emits when the parent has stopped emitting for [[delayFromLastEventMs]] ms.
    */
  override protected[airstream] def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    maybeLastTimeoutHandle.foreach(js.timers.clearTimeout)
    maybeLastTimeoutHandle = js.defined(
      js.timers.setTimeout(delayFromLastEventMs.toDouble) {
        //println(s"> init trx from DebounceEventStream.onTry($nextValue)")
        new Transaction(fireTry(nextValue, _))
      }
    )
  }

  override protected[this] def onStop(): Unit = {
    maybeLastTimeoutHandle.foreach(js.timers.clearTimeout)
    maybeLastTimeoutHandle = js.undefined
    super.onStop()
  }
}
