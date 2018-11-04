package com.raquo.airstream.eventbus

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.{Owned, Owner}

// @TODO[Naming] Maybe something with "Subscription"? Although nah...
class EventBusSource[A](
  eventBusStream: EventBusStream[A],
  val sourceStream: EventStream[A],
  override protected[this] val owner: Owner
) extends Owned {

  init()

  eventBusStream.addSource(this)

  override protected[this] def onKilled(): Unit = {
    eventBusStream.removeSource(this)
  }

  /** Remove this source stream from this event bus */
  override def kill(): Unit = super.kill()
}
