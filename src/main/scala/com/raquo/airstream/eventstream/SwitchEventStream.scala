package com.raquo.airstream.eventstream

import com.raquo.airstream.core.{InternalObserver, MemoryObservable, Observable, Transaction}
import com.raquo.airstream.features.{InternalNextErrorObserver, SingleParentObservable}

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** `parent` observable emits values that we convert into streams using `makeStream`.
  *
  * This stream emits the events from the last such stream created this way.
  *
  * Events are emitted at the same time as the currently tracked stream emits them.
  *
  * When `parent` emits a nextValue, this stream switches to emitting events from `makeStream(nextValue)` (which is a stream).
  *
  * If parent stream emits an error, this stream re-emits that error and unsubscribes from the last emitted stream
  *
  * Warning: Similar to [[com.raquo.airstream.eventbus.EventBus]], this stream emits events in
  * a new transaction because its proper topoRank would need to be dynamic, which we don't support.
  *
  * Note: this stream loses its memory if stopped.
  *
  * @param makeStream Note: Must not throw
  */
class SwitchEventStream[I, O](
  override protected[this] val parent: Observable[I],
  makeStream: I => EventStream[O]
) extends EventStream[O] with SingleParentObservable[I, O] with InternalNextErrorObserver[I] {

  override protected[airstream] val topoRank: Int = 1

  private[this] var maybeCurrentEventStream: js.UndefOr[Try[EventStream[O]]] = parent match {
    case mo: MemoryObservable[I @unchecked] => mo.tryNow().map(makeStream)
    case _ => js.undefined
  }

  // @TODO[Elegance] Maybe we should abstract away this kind of internal observer
  private[this] val internalEventObserver: InternalObserver[O] = InternalObserver[O](
    onNext = (nextEvent, _) => new Transaction(fireValue(nextEvent, _)),
    onError = (nextError, _) => new Transaction(fireError(nextError, _))
  )

  override protected[airstream] def onNext(nextValue: I, transaction: Transaction): Unit = {
    removeInternalObserverFromCurrentEventStream()
    val nextStream = makeStream(nextValue)
    maybeCurrentEventStream = Success(nextStream)
    // If we're receiving events, this stream is started, so no need to check for that
    nextStream.addInternalObserver(internalEventObserver)
  }

  override protected[airstream] def onError(nextError: Throwable, transaction: Transaction): Unit = {
    removeInternalObserverFromCurrentEventStream()
    maybeCurrentEventStream = Failure(nextError)
  }

  override protected[this] def onStart(): Unit = {
    maybeCurrentEventStream.foreach(_.foreach(_.addInternalObserver(internalEventObserver)))
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    removeInternalObserverFromCurrentEventStream()
    maybeCurrentEventStream = js.undefined
    super.onStop()
  }

  private def removeInternalObserverFromCurrentEventStream(): Unit = {
    maybeCurrentEventStream.foreach { _.foreach { currentStream =>
      Transaction.removeInternalObserver(currentStream, internalEventObserver)
    }}
  }

}
