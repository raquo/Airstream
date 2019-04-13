package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction

import scala.util.Try

// @TODO[Airstream] needs testing

// @TODO[API] This looks like a kludge only relevant to streams-only libraries. Reconsider if we want to include this at all.
// @TODO[API] Is it desirable that we re-emit these events when this stream is re-started, or should that only happen once?
// @TODO[API] This is how XStream does it, I haven't compared with other libs
/** This event stream emits a sequence of events every time it is started */
class SeqEventStream[A](events: Seq[Try[A]], emitOnce: Boolean) extends EventStream[A] {

  private[this] var hasEmitted = false

  override protected[airstream] val topoRank: Int = 1

  override protected[this] def onStart(): Unit = {
    super.onStart()
    if (!emitOnce || !hasEmitted) {
      events.foreach(event => new Transaction(fireTry(event, _)))
    }
    if (!hasEmitted) {
      hasEmitted = true
    }
  }

}
