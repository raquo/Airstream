package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.features.{InternalNextErrorObserver, SingleParentObservable}

import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle

class DelayEventStream[A](
  override protected val parent: EventStream[A],
  delayMillis: Int
) extends EventStream[A] with SingleParentObservable[A, A] with InternalNextErrorObserver[A] {

  /** Async stream, so reset rank */
  override protected[airstream] val topoRank: Int = 1

  private val timerHandles: js.Array[SetTimeoutHandle] = js.Array()

  override protected[airstream] def onNext(nextValue: A, transaction: Transaction): Unit = {
    var timerHandle: SetTimeoutHandle = null
    timerHandle = js.timers.setTimeout(delayMillis.toDouble) {
      //println(s"> init trx from DelayEventStream.onNext($nextValue)")
      timerHandles.splice(timerHandles.indexOf(timerHandle), deleteCount = 1) // Remove handle
      new Transaction(fireValue(nextValue, _))
    }
    timerHandles.push(timerHandle)
  }

  override def onError(nextError: Throwable, transaction: Transaction): Unit = {
    var timerHandle: SetTimeoutHandle = null
    timerHandle = js.timers.setTimeout(delayMillis.toDouble) {
      timerHandles.splice(timerHandles.indexOf(timerHandle), deleteCount = 1) // Remove handle
      new Transaction(fireError(nextError, _))
    }
    timerHandles.push(timerHandle)
  }

  override protected[this] def onStop(): Unit = {
    timerHandles.foreach(js.timers.clearTimeout)
    timerHandles.length = 0 // Clear array
    super.onStop()
  }
}
