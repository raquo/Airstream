package com.raquo.airstream.flatten

import com.raquo.airstream.core.{EventStream, Observable, Signal}

/** [[Observable.MetaObservable.flatten]] needs an instance of this trait to know how exactly to do the flattening. */
trait FlattenStrategy[-Outer[+_] <: Observable[_], -Inner[_], +Output[+_] <: Observable[_]] {
  /** Must not throw */
  def flatten[A](parent: Outer[Inner[A]]): Output[A]
}

object FlattenStrategy {

  /** See docs for [[SwitchStream]] */
  object SwitchStreamStrategy extends FlattenStrategy[Observable, EventStream, EventStream] {
    override def flatten[A](parent: Observable[EventStream[A]]): EventStream[A] = {
      new SwitchStream[EventStream[A], A](parent = parent, makeStream = identity)
    }
  }

  /** See docs for [[ConcurrentStream]] */
  object ConcurrentStreamStrategy extends FlattenStrategy[Observable, EventStream, EventStream] {
    override def flatten[A](parent: Observable[EventStream[A]]): EventStream[A] = {
      new ConcurrentStream[A](parent = parent)
    }
  }

  /** See docs for [[SwitchSignalStream]] */
  object SwitchSignalStreamStrategy extends FlattenStrategy[EventStream, Signal, EventStream] {
    override def flatten[A](parent: EventStream[Signal[A]]): EventStream[A] = {
      new SwitchSignalStream(parent)
    }
  }

  /** See docs for [[SwitchSignal]] */
  object SwitchSignalStrategy extends FlattenStrategy[Signal, Signal, Signal] {
    override def flatten[A](parent: Signal[Signal[A]]): Signal[A] = {
      new SwitchSignal(parent)
    }
  }

}
