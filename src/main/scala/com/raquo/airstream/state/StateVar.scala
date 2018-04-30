package com.raquo.airstream.state

import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.ownership.Owner

// @TODO[Test] Test this

class StateVar[A] private(initial: A, owner: Owner) {

  private val eventBus = new EventBus[A]

  val writer: WriteBus[A] = eventBus.writer

  val state: State[A] = new MapState[A, A](
    parent = eventBus.events.toSignal(initial),
    project = identity,
    owner
  )
}

object StateVar {

  def apply[A](initial: A)(implicit owner: Owner): StateVar[A] = new StateVar(initial, owner)
}
