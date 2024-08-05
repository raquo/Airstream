package com.raquo.airstream.timing

import com.raquo.airstream.common.{InternalTryObserver, SingleParentStream}
import com.raquo.airstream.core.{EventStream, Transaction}

import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.util.Try

/** [[ThrottleStream]] emits at most one event per `intervalMs`.
  *   - All events are emitted in a new transaction, after an async delay, even
  *     if the delay is zero ms
  *   - Any incoming event is scheduled to be emitted as soon as possible, but
  *     no sooner than `intervalMs` after the last event that was actually
  *     emitted by the throttled stream
  *   - When an event is scheduled to be emitted, any event that was previously
  *     scheduled is cancelled (that's the nature of throttling, you only get at
  *     most one event within `intervalMs`)
  *   - Errors are propagated in the same manner
  *   - Stopping the stream cancels scheduled events and makes it forget
  *     everything that happened before.
  *
  * See also See also [[DebounceStream]]
  */
class ThrottleStream[A](
    override protected[this] val parent: EventStream[A],
    intervalMs: Int,
    leading: Boolean
) extends SingleParentStream[A, A]
    with InternalTryObserver[A] {

  private[this] var lastEmittedEventMs: js.UndefOr[Double] = js.undefined

  /** Note: we unset this after it's done */
  private[this] var maybeFirstTimeoutHandle: js.UndefOr[SetTimeoutHandle] =
    js.undefined

  private[this] var maybeLastTimeoutHandle: js.UndefOr[SetTimeoutHandle] =
    js.undefined

  override protected val topoRank: Int = 1

  override protected def onTry(
      nextValue: Try[A],
      transaction: Transaction
  ): Unit = {

    val nowMs = js.Date.now()

    val remainingMs = lastEmittedEventMs.fold(
      ifEmpty = if (leading) 0 else intervalMs
    )(lastEventMs => {
      val msSinceLastEvent = nowMs - lastEventMs
      js.Math.max(intervalMs - msSinceLastEvent.toInt, 0)
    })

    if (leading && lastEmittedEventMs.isEmpty) {
      // #Note lastEmittedEventMs is an approximation (compare to the `else` case), I hope that doesn't bite us
      lastEmittedEventMs = nowMs

      maybeFirstTimeoutHandle = js.timers.setTimeout(0) {
        maybeFirstTimeoutHandle = js.undefined
        // println(s"> init trx from leading ThrottleEventStream.onTry($nextValue)")
        Transaction(fireTry(nextValue, _))
      }

    } else {
      maybeLastTimeoutHandle.foreach(js.timers.clearTimeout)

      maybeLastTimeoutHandle = js.timers.setTimeout(remainingMs.toDouble) {
        lastEmittedEventMs =
          js.Date
            .now() // @TODO Should this fire now, or inside the transaction below?
        // println(s"> init trx from ThrottleEventStream.onTry($nextValue)")
        Transaction(fireTry(nextValue, _))
      }
    }
  }

  override protected[this] def onStop(): Unit = {
    maybeFirstTimeoutHandle.foreach(js.timers.clearTimeout)
    maybeLastTimeoutHandle.foreach(js.timers.clearTimeout)
    maybeFirstTimeoutHandle = js.undefined
    maybeLastTimeoutHandle = js.undefined
    lastEmittedEventMs = js.undefined
    super.onStop()
  }

}
