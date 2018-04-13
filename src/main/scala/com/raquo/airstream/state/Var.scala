package com.raquo.airstream.state

import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.ownership.Owner

// @TODO[Test] Test this

class Var[A] private(
  initial: A,
  eventBus: EventBus[A],
  owner: Owner
) extends MapState[A, A](
  parent = eventBus.events.toSignal(initial),
  project = identity,
  owner
) {
  val writer: WriteBus[A] = eventBus.writer
}

object Var {

  def apply[A](initial: A)(implicit owner: Owner): Var[A] = {
    val bus = new EventBus[A]
    new Var(initial, bus, owner)
  }
}
