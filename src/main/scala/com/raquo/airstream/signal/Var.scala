package com.raquo.airstream.signal

import com.raquo.airstream.eventbus.{EventBus, WriteBus}

import scala.util.{Success, Try}

class Var[A] private(initial: Try[A]) {

  private val eventBus = new EventBus[A]

  val writer: WriteBus[A] = eventBus.writer

  val signal: Signal[A] = eventBus.events.toSignalWithTry(initial)
}

object Var {

  def apply[A](initial: A): Var[A] = fromTry(Success(initial))

  @inline def fromTry[A](initial: Try[A]): Var[A] = new Var(initial)
}

