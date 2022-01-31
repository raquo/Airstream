package com.raquo.airstream.timing

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentEventStream}
import com.raquo.airstream.core.{EventStream, Transaction}
import com.raquo.ew.JsArray

import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle

class DelayEventStream[A](
  override protected[this] val parent: EventStream[A],
  delayMs: Int
) extends SingleParentEventStream[A, A] with InternalNextErrorObserver[A] {

  /** Async stream, so reset rank */
  override protected val topoRank: Int = 1

  private val timerHandles: JsArray[SetTimeoutHandle] = JsArray()

  override protected def onNext(nextValue: A, transaction: Transaction): Unit = {
    var timerHandle: SetTimeoutHandle = null
    timerHandle = js.timers.setTimeout(delayMs.toDouble) {
      //println(s"> init trx from DelayEventStream.onNext($nextValue)")
      timerHandles.splice(timerHandles.indexOf(timerHandle), deleteCount = 1) // Remove handle
      new Transaction(fireValue(nextValue, _))
      ()
    }
    timerHandles.push(timerHandle)
  }

  override def onError(nextError: Throwable, transaction: Transaction): Unit = {
    var timerHandle: SetTimeoutHandle = null
    timerHandle = js.timers.setTimeout(delayMs.toDouble) {
      timerHandles.splice(timerHandles.indexOf(timerHandle), deleteCount = 1) // Remove handle
      new Transaction(fireError(nextError, _))
      ()
    }
    timerHandles.push(timerHandle)
  }

  override protected[this] def onStop(): Unit = {
    timerHandles.forEach(js.timers.clearTimeout)
    timerHandles.length = 0 // Clear array
    super.onStop()
  }
}
