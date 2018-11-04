package com.raquo.airstream.state

import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.ownership.Owner

import scala.util.{Success, Try}

// @TODO[Test] Test this

class StateVar[A] private(initial: Try[A], owner: Owner) {

  private val eventBus = new EventBus[A]

  val writer: WriteBus[A] = eventBus.writer

  val state: State[A] = new MapState[A, A](
    parent = eventBus.events.toSignalWithTry(initial),
    project = identity,
    recover = None,
    owner
  )
}

object StateVar {

  def apply[A](initial: A)(implicit owner: Owner): StateVar[A] = fromTry(Success(initial))(owner)

  @inline def fromTry[A](initial: Try[A])(implicit owner: Owner): StateVar[A] = new StateVar(initial, owner)
}
