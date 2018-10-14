package com.raquo.airstream.features

import com.raquo.airstream.core.{LazyObservable, Observable}
import com.raquo.airstream.eventstream.{ConcurrentFutureStream, EventStream, FutureEventStream, SwitchEventStream}

import scala.concurrent.Future

/** [[Observable.MetaObservable.flatten]] needs an instance of this trait to know how exactly to do the flattening. */
trait FlattenStrategy[-Outer[+_] <: Observable[_], -Inner[_], Output[+_] <: LazyObservable[_]] {
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

  /** See docs for [[SwitchEventStream]] */
  object SwitchFutureStrategy extends FlattenStrategy[Observable, Future, EventStream] {
    override def flatten[A](parent: Observable[Future[A]]): EventStream[A] = {
      new SwitchEventStream[Future[A], A](
        parent = parent,
        makeStream = new FutureEventStream(_, emitIfFutureCompleted = true)
      )
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
