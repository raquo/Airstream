package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentStream}
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

/** Event stream that mimics the parent event stream, except that first it skips (drops) the parent's events, for as
  * long as `dropWhile` returns true. As soon as it returns false for the first time, it starts mirroring the parent
  * stream faithfully.
  *
  * Note: only events are dropped, not errors.
  *
  * @param dropWhile     nextEvent => shouldDrop
  *                      Function which determines whether this stream should drop the given event.
  *                      Warning: MUST NOT THROW!
  *
  * @param reset         This is called when this stream is stopped if resetOnStop is true. Use it to
  *                      reset your `dropWhile` function's internal state, if needed.
  *                      Warning: MUST NOT THROW!
  *
  * @param resetOnStop   If true, stopping this stream will reset the stream's memory of previously
  *                      dropped events (up to you to implement the `reset` as far as your `dropWhile`
  *                      function is concerned though).
  */
class DropStream[A](
  override protected val parent: EventStream[A],
  dropWhile: A => Boolean,
  reset: () => Unit,
  resetOnStop: Boolean
) extends SingleParentStream[A, A] with InternalNextErrorObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  private var disableDropping: Boolean = false

  override protected def onNext(nextValue: A, transaction: Transaction): Unit = {
    val shouldDropNextValue = !disableDropping && {
      val dropNext = dropWhile(nextValue)
      disableDropping = !dropNext
      dropNext
    }
    if (!shouldDropNextValue) {
      fireValue(nextValue, transaction)
    } else {
      //println(s"!!! DROPPED event `$nextParentValue` from ${this}. Total drops: $numDropped")
    }
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }

  override protected[this] def onStop(): Unit = {
    if (resetOnStop) {
      disableDropping = false
      reset()
    }
    super.onStop()
  }
}
