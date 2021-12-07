package com.raquo.airstream.timing

import com.raquo.airstream.core.{Transaction, WritableEventStream}

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** @param next (currentState => (nextState, nextIntervalMs)
  *             Note: guarded against exceptions.
  *             If `next` throws, stream will emit that error
  */
class PeriodicEventStream[A](
  initial: A,
  next: A => Option[(A, Int)],
  resetOnStop: Boolean
) extends WritableEventStream[A] {

  override protected val topoRank: Int = 1

  private var currentValue: A = initial

  private var maybeTimeoutHandle: js.UndefOr[js.timers.SetTimeoutHandle] = js.undefined

  // @TODO[API] Not a fan of exposing the ability to write to a stream on the stream itself,
  //  we separate this out on EventBus and Var
  def resetTo(value: A): Unit = {
    resetTo(value, tickNext = true)
  }

  private def resetTo(value: A, tickNext: Boolean): Unit = {
    clearTimeout()
    currentValue = value
    if (tickNext && isStarted) {
      tick()
    }
  }

  private def clearTimeout(): Unit = {
    maybeTimeoutHandle.foreach(js.timers.clearTimeout)
    maybeTimeoutHandle = js.undefined
  }

  private def tick(): Unit = {
    new Transaction(trx => { // #Note[onStart,trx,async]
      fireValue(currentValue, trx)
      setNext()
    })
  }

  private def setNext(): Unit = {
    Try(next(currentValue)) match {
      case Success(Some((nextValue, nextIntervalMs))) =>
        currentValue = nextValue
        maybeTimeoutHandle = js.timers.setTimeout(nextIntervalMs.toDouble) {
          tick()
        }
      case Success(None) =>
        resetTo(initial, tickNext = false)
      case Failure(err) =>
        new Transaction(fireError(err, _)) // #Note[onStart,trx,async]
    }
  }

  override protected def onWillStart(): Unit = () // noop

  override protected[this] def onStart(): Unit = {
    super.onStart()
    tick()
  }

  override protected[this] def onStop(): Unit = {
    super.onStop()
    clearTimeout()
    if (resetOnStop) {
      resetTo(initial)
    }
  }
}
