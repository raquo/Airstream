package com.raquo.airstream.eventbus

import com.raquo.airstream.core.Source.EventSource
import com.raquo.airstream.core.{EventStream, Named, Observer, Sink}

import scala.util.Try

/** EventBus combines a WriteBus and a stream of its events.
  *
  * `writer` and `events` are made separate to allow you to manage permissions.
  * For example, you can pass only the `writer` instance to a function that
  * should only have access to writing events, not reading all events from the bus.
  */
class EventBus[A] extends EventSource[A] with Sink[A] with Named {

  val writer: WriteBus[A] = new WriteBus[A]

  val events: EventStream[A] = writer.stream

  def emit(event: A): Unit = writer.onNext(event)

  def emitTry(event: Try[A]): Unit = writer.onTry(event)

  override def toObservable: EventStream[A] = events

  override def toObserver: Observer[A] = writer
}

object EventBus {

  type EventBusTuple[A] = (EventBus[A], A)

  type EventBusTryTuple[A] = (EventBus[A], Try[A])

  // @TODO[Integrity] Not sure how to implement these without .asInstanceOf

  /** Emit events into several EventBus-es at once (in the same transaction)
    * Example usage: emitTry(eventBus1 -> value1, eventBus2 -> value2)
    */
  def emit[A](
    values: EventBusTuple[A]*
  ): Unit = {
    WriteBus.emit(values.map(value => (value._1.writer, value._2)): _*)
  }

  /** Emit events into several WriteBus-es at once (in the same transaction)
    * Example usage: emitTry(eventBus1 -> Success(value1), eventBus2 -> Failure(error2))
    */
  def emitTry[A](
    values: EventBusTryTuple[A]*
  ): Unit = {
    WriteBus.emitTry(values.map(value => (value._1.writer, value._2)): _*)
  }
}
