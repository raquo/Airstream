package com.raquo.airstream.features

import com.raquo.airstream.core.Observable
import com.raquo.airstream.eventstream.{ConcurrentFutureStream, EventStream, FutureEventStream, SwitchEventStream}

import scala.concurrent.Future

/** [[Observable.MetaObservable.flatten]] needs an instance of this trait to know how exactly to do the flattening. */
trait FlattenStrategy[Outer[_], Inner[_], Output[_]] {

  /** Must not throw */
  def flatMap[A, B](outer: Outer[A], project: A => Inner[B]): Output[B]
}

object FlattenStrategy {

  object SwitchStreamStrategy extends FlattenStrategy[Observable, EventStream, EventStream] {
    override def flatMap[A, B](outer: Observable[A], project: A => EventStream[B]): EventStream[B] = {
      new SwitchEventStream[EventStream[B], B](parent = outer.map(project), makeStream = identity)
    }
  }

  /** See docs for [[SwitchEventStream]] */
  object SwitchFutureStrategy extends FlattenStrategy[Observable, Future, EventStream] {
    override def flatMap[A, B](outer: Observable[A], project: A => Future[B]): EventStream[B] = {
      new SwitchEventStream[A, B](
        parent = outer,
        makeStream = a => new FutureEventStream(project(a), emitIfFutureCompleted = true)
      )
    }
  }

  /** See docs for [[ConcurrentFutureStream]] */
  object ConcurrentFutureStrategy extends FlattenStrategy[Observable, Future, EventStream] {
    override def flatMap[A, B](outer: Observable[A], project: A => Future[B]): EventStream[B] = {
      new ConcurrentFutureStream[B](outer.map(project), dropPreviousValues = false, emitIfFutureCompleted = true)
    }
  }

  // @TODO[Naming] this strategy needs a better name
  /** See docs for [[ConcurrentFutureStream]] */
  object OverwriteFutureStrategy extends FlattenStrategy[Observable, Future, EventStream] {
    override def flatMap[A, B](outer: Observable[A], project: A => Future[B]): EventStream[B] = {
      new ConcurrentFutureStream[B](outer.map(project), dropPreviousValues = true, emitIfFutureCompleted = true)
    }
  }
}
