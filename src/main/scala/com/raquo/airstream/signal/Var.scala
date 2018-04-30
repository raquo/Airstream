package com.raquo.airstream.signal

import com.raquo.airstream.eventbus.{EventBus, WriteBus}

class Var[A] private(initial: A) {

  private val eventBus = new EventBus[A]

  val writer: WriteBus[A] = eventBus.writer

  val signal: Signal[A] = new MapSignal[A, A](
    parent = eventBus.events.toSignal(initial),
    project = identity
  )
}

object Var {

  def apply[A](initial: A): Var[A] = new Var(initial)
}

