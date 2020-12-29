package com.raquo.airstream.features

import com.raquo.airstream.core.Observable
import com.raquo.airstream.eventstream.{ConcurrentEventStream, ConcurrentFutureStream, EventStream, SwitchEventStream}
import com.raquo.airstream.signal.{Signal, SwitchSignal}

import scala.concurrent.Future

/** [[Observable.MetaObservable.flatten]] needs an instance of this trait to know how exactly to do the flattening. */
trait FlattenStrategy[-Outer[+_] <: Observable[_], -Inner[_], Output[+_] <: Observable[_]] {
  /** Must not throw */
  def flatten[A](parent: Outer[Inner[A]]): Output[A]
}

object FlattenStrategy {

  /** See docs for [[SwitchEventStream]] */
  object SwitchStreamStrategy extends FlattenStrategy[Observable, EventStream, EventStream] {
    override def flatten[A](parent: Observable[EventStream[A]]): EventStream[A] = {
      new SwitchEventStream[EventStream[A], A](parent = parent, makeStream = identity)
    }
  }

  /** See docs for [[ConcurrentEventStream]] */
  object ConcurrentStreamStrategy extends FlattenStrategy[Observable, EventStream, EventStream] {
    override def flatten[A](parent: Observable[EventStream[A]]): EventStream[A] = {
      new ConcurrentEventStream[A](parent = parent)
    }
  }

  /** See docs for [[SwitchEventStream]] */
  object SwitchFutureStrategy extends FlattenStrategy[Observable, Future, EventStream] {
    override def flatten[A](parent: Observable[Future[A]]): EventStream[A] = {
      new SwitchEventStream[Future[A], A](
        parent = parent,
        makeStream = EventStream.fromFuture(_, emitFutureIfCompleted = true)
      )
    }
  }

  /** See docs for [[SwitchSignal]] */
  object SwitchSignalStrategy extends FlattenStrategy[Signal, Signal, Signal] {
    override def flatten[A](parent: Signal[Signal[A]]): Signal[A] = {
      new SwitchSignal(parent)
    }
  }

  /** See docs for [[ConcurrentFutureStream]] */
  object ConcurrentFutureStrategy extends FlattenStrategy[Observable, Future, EventStream] {
    override def flatten[A](parent: Observable[Future[A]]): EventStream[A] = {
      new ConcurrentFutureStream[A](parent, dropPreviousValues = false, emitIfFutureCompleted = true)
    }
  }

  // @TODO[Naming] this strategy needs a better name
  /** See docs for [[ConcurrentFutureStream]] */
  object OverwriteFutureStrategy extends FlattenStrategy[Observable, Future, EventStream] {
    override def flatten[A](parent: Observable[Future[A]]): EventStream[A] = {
      new ConcurrentFutureStream[A](parent, dropPreviousValues = true, emitIfFutureCompleted = true)
    }
  }
}
