package com.raquo.airstream.flatten

import com.raquo.airstream.common.InternalNextErrorObserver
import com.raquo.airstream.core.{EventStream, InternalObserver, Observable, Protected, Signal, Transaction, WritableStream}
import com.raquo.ew.JsArray

import scala.util.{Failure, Success}

/** This is essentially a dynamic version of `EventStream.merge`.
  * - The resulting stream re-emits all the events emitted by all of the streams
  *   previously emitted by the input observable.
  * - If you restart the resulting stream, it will remember and resubscribe to all of the
  *   streams it previously listened to.
  * - If the input observable emits the same stream more than once, that stream will only be added once.
  */
class ConcurrentStream[A](
  parent: Observable[EventStream[A]]
) extends WritableStream[A] with InternalNextErrorObserver[EventStream[A]] {

  private val accumulatedStreams: JsArray[EventStream[A]] = JsArray()

  private val internalEventObserver: InternalObserver[A] = InternalObserver[A](
    onNext = (nextEvent, _) => new Transaction(fireValue(nextEvent, _)),
    onError = (nextError, _) => new Transaction(fireError(nextError, _))
  )

  override protected val topoRank: Int = 1

  override protected def onWillStart(): Unit = {
    Protected.maybeWillStart(parent)
    accumulatedStreams.forEach(Protected.maybeWillStart(_))
    parent match {
      case signal: Signal[EventStream[A @unchecked] @unchecked] =>
        signal.tryNow() match {
          case Success(stream) =>
            // We add internal observer later, in `onStart`. onWillStart should not start any observables. // #nc[doc] this pattern
            maybeAddStream(stream, addInternalObserver = false)
          case _ => ()
        }
      case _ => ()
    }
  }

  override protected[this] def onStart(): Unit = {
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)
    accumulatedStreams.forEach(_.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false))
    parent match {
      case signal: Signal[EventStream[A @unchecked] @unchecked] =>
        signal.tryNow() match {
          case Failure(err) =>
            // @TODO[API] Not 100% sure that we should emit this error, but since
            //  we expect to use signal's current value, I think this is right.
            new Transaction(fireError(err, _)) // #Note[onStart,trx,loop]
          case _ => ()
        }
      case _ => ()
    }
  }

  override protected[this] def onStop(): Unit = {
    accumulatedStreams.forEach(_.removeInternalObserver(internalEventObserver))
    parent.removeInternalObserver(observer = this)
    super.onStop()
  }

  override protected def onNext(nextStream: EventStream[A], transaction: Transaction): Unit = {
    maybeAddStream(nextStream, addInternalObserver = true)
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }

  private def maybeAddStream(stream: EventStream[A], addInternalObserver: Boolean): Unit = {
    if (accumulatedStreams.indexOf(stream) == -1) {
      accumulatedStreams.push(stream)
      if (addInternalObserver) {
        stream.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = true)
      }
    }
  }

}
