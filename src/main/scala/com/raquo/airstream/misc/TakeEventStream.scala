package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentEventStream}
import com.raquo.airstream.core.{EventStream, Protected, Transaction}

/** Event stream that mimics the parent event stream (both events and errors) for as long as `takeWhile` returns true.
  * As soon as `takeWhile` returns `false` for the first time, it stops emitting anything.
  *
  * @param takeWhile     nextEvent => shouldDrop
  *                      Function which determines whether this stream should take the given event.
  *                      Warning: MUST NOT THROW!
  *
  * @param reset         This is called when this stream is stopped if resetOnStop is true. Use it to
  *                      reset your `takeWhile` function's internal state, if needed.
  *                      Warning: MUST NOT THROW!
  *
  * @param resetOnStop   If true, stopping this stream will reset the stream's memory of previously
  *                      taken events (up to you to implement the `reset` as far as your `takeWhile`
  *                      function is concerned though).
  */
class TakeEventStream[A](
  override protected val parent: EventStream[A],
  takeWhile: A => Boolean,
  reset: () => Unit,
  resetOnStop: Boolean
) extends SingleParentEventStream[A, A] with InternalNextErrorObserver[A] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  private var disableTaking: Boolean = false

  override protected def onNext(nextValue: A, transaction: Transaction): Unit = {
    val shouldTakeNextValue = !disableTaking && {
      val takeNext = takeWhile(nextValue)
      disableTaking = !takeNext
      takeNext
    }
    if (shouldTakeNextValue) {
      fireValue(nextValue, transaction)
    } else {
      //println(s"!!! DROPPED event `$nextParentValue` from ${this}. Total drops: $numDropped")
    }
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    if (!disableTaking) {
      fireError(nextError, transaction)
    }
  }

  override protected[this] def onStop(): Unit = {
    if (resetOnStop) {
      disableTaking = false
      reset()
    }
    super.onStop()
  }
}
