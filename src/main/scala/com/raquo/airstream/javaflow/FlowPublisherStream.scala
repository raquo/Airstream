package com.raquo.airstream.javaflow

import com.raquo.airstream.core.EventStream

import java.util.concurrent.Flow

object FlowPublisherStream {

  def apply[A](publisher: Flow.Publisher[A], emitOnce: Boolean = false): EventStream[A] = {
    var subscription: Flow.Subscription = null

    EventStream.fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, fireError, _, _) => {
        val subscriber = new Flow.Subscriber[A] {
          def onNext(value: A): Unit = fireEvent(value)
          def onError(err: Throwable): Unit = fireError(err)
          def onComplete(): Unit = ()
          def onSubscribe(sub: Flow.Subscription): Unit = {
            sub.request(Long.MaxValue) // unlimited demand for events
            subscription = sub
          }
        }
        publisher.subscribe(subscriber)
      },
      stop = _ => {
        if (subscription ne null) {
          subscription.cancel()
          subscription = null
        }
      }
    )
  }
}
