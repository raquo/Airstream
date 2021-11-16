package com.raquo.airstream.flatten

import com.raquo.airstream.common.{ InternalNextErrorObserver, SingleParentObservable }
import com.raquo.airstream.core.{ EventStream, InternalObserver, Observable, Signal, Transaction, WritableEventStream }

import scala.scalajs.js
import scala.util.{ Failure, Success }

/** This is essentially a dynamic version of `EventStream.merge`.
  * - The resulting stream re-emits all the events emitted by all of the streams
  *   previously emitted by the input observable.
  * - If you stop observing the resulting stream, it will forget all of the
  *   streams it previously listened to.
  * - When you start it up again, it will start listening to the input observable
  *   from scratch, as if it's the first time you started it.
  * */
class ConcurrentEventStream[A](
  override protected[this] val parent: Observable[EventStream[A]]
) extends WritableEventStream[A] with SingleParentObservable[EventStream[A], A] with InternalNextErrorObserver[EventStream[A]] {

  private val accumulatedStreams: js.Array[EventStream[A]] = js.Array()

  private val internalEventObserver: InternalObserver[A] = InternalObserver[A](
    onNext = (nextEvent, _) => new Transaction(fireValue(nextEvent, _)),
    onError = (nextError, _) => new Transaction(fireError(nextError, _))
  )

  override protected val topoRank: Int = 1

  override protected[this] def onStart(): Unit = {
    parent match {
      case signal: Signal[EventStream[A] @unchecked] =>
        signal.tryNow() match {
          case Success(initialStream) =>
            addStream(initialStream)
          case Failure(err) =>
            // @TODO[API] Not 100% that we should emit this error, but since
            //  we expect to use signal's current value, I think this is right.
            new Transaction(fireError(err, _))
        }
      case _ => js.undefined
    }
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    accumulatedStreams.foreach(Transaction.removeInternalObserver(_, internalEventObserver))
    accumulatedStreams.clear()
    super.onStop()
  }

  override protected def onNext(nextStream: EventStream[A], transaction: Transaction): Unit = {
    addStream(nextStream)
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }

  private def addStream(stream: EventStream[A]): Unit = {
    accumulatedStreams.push(stream)
    stream.addInternalObserver(internalEventObserver)
  }
}
