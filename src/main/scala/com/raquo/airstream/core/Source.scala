package com.raquo.airstream.core

/** A Source is something that can be converted to an [[Observable]].
  * The counterparty to Source is a [[Sink]], something that can be converted to an [[Observer]].
  *
  * A Source could be an Observable itself, an EventBus, a Var, or, via implicits, a third party type like Future or ZIO.
  *
  * The point of using Source instead of Observable in your API is to let the end users
  * pass simply `eventBus` instead of `eventBus.events` to a method that requires Source,
  * and to achieve that without having an implicit conversion from EventBus to Observable,
  * because then you'd also want an implicit conversion from EventBus to Observer, and
  * those two would be incompatible (e.g. both Observable and Observer have a filter method).
  */
trait Source[+A] {
  def toObservable: Observable[A]
}

object Source {

  trait EventSource[+A] extends Source[A] {

    override def toObservable: EventStream[A]
  }

  trait SignalSource[+A] extends Source[A] {

    override def toObservable: Signal[A]
  }

  // #TODO[API] Disabled integrations, let's see if anyone complains. These conversions are unfortunately not smooth enough to be implicit.

  //implicit def futureToEventSource[A](future: Future[A]): EventSource[A] = EventStream.fromFuture(future)
  //
  //implicit def futureToSignalSource[A](future: Future[A]): SignalSource[Option[A]] = Signal.fromFuture(future)
  //
  //implicit def jsPromiseToEventSource[A](promise: js.Promise[A]): EventSource[A] = EventStream.fromJsPromise(promise)
  //
  //implicit def jsPromiseToSignalSource[A](promise: js.Promise[A]): SignalSource[Option[A]] = Signal.fromJsPromise(promise)

}
