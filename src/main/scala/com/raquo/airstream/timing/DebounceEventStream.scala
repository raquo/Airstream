package com.raquo.airstream.timing

import com.raquo.airstream.common.{ InternalTryObserver, SingleParentObservable }
import com.raquo.airstream.core.{ EventStream, Transaction, WritableEventStream }

import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.util.Try

// @TODO[Test] Verify debounce

/** This stream emits the last event emitted by `parent`, but only after `intervalMs` has elapsed
  * since `parent` emitted the previous event.
  *
  * Essentially, this stream emits the parent's last event, but only once the parent stops emitting
  * events for `intervalMs`.
  *
  * See also [[ThrottleEventStream]]
  */
class DebounceEventStream[A](
  override protected[this] val parent: EventStream[A],
  intervalMs: Int
) extends WritableEventStream[A] with SingleParentObservable[A, A] with InternalTryObserver[A] {

  private[this] var maybeLastTimeoutHandle: js.UndefOr[SetTimeoutHandle] = js.undefined

  override protected[airstream] val topoRank: Int = 1

  /** Every time [[parent]] emits an event, we clear the previous timer and set a new one.
    * This stream only emits when the parent has stopped emitting for [[intervalMs]] ms.
    */
  override protected[airstream] def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    maybeLastTimeoutHandle.foreach(js.timers.clearTimeout)
    maybeLastTimeoutHandle = js.defined(
      js.timers.setTimeout(intervalMs.toDouble) {
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
