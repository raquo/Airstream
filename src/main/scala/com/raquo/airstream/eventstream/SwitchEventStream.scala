package com.raquo.airstream.eventstream

import com.raquo.airstream.core.{InternalObserver, MemoryObservable, Observable, Transaction}
import com.raquo.airstream.features.SingleParentObservable

import scala.scalajs.js

/** `parent` observable emits values that we convert into streams using `makeStream`.
  *
  * This stream emits the events from the last such stream created this way.
  *
  * Events are emitted at the same time as the currently tracked stream emits them.
  *
  * When `parent` emits a nextValue, this stream switches to emitting events from `makeStream(nextValue)` (which is a stream).
  *
  * Warning: Similar to [[com.raquo.airstream.eventbus.EventBus]], this stream emits events in
  * a new transaction because its proper topoRank would need to be dynamic, which we don't support.
  *
  * Note: this stream loses its memory if stopped.
  */
class SwitchEventStream[I, O](
  override protected[this] val parent: Observable[I],
  makeStream: I => EventStream[O]
) extends EventStream[O] with SingleParentObservable[I, O] {

  override protected[airstream] val topoRank: Int = 1

  private[this] var maybeCurrentEventStream: js.UndefOr[EventStream[O]] = parent match {
    case mo: MemoryObservable[I @unchecked] => makeStream(mo.now())
    case _ => js.undefined
  }

  private[this] val internalEventObserver: InternalObserver[O] = InternalObserver {
    (event, _) => new Transaction(fire(event, _))
  }

  override protected[airstream] def onNext(nextValue: I, transaction: Transaction): Unit = {
    removeInternalObserverFromCurrentEventStream()
    val nextStream = makeStream(nextValue)
    maybeCurrentEventStream = nextStream
    // If we're receiving events, this stream is started, so no need to check for that
    nextStream.addInternalObserver(internalEventObserver)
  }

  override protected[this] def onStart(): Unit = {
    maybeCurrentEventStream.foreach(_.addInternalObserver(internalEventObserver))
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    removeInternalObserverFromCurrentEventStream()
    maybeCurrentEventStream = js.undefined
    super.onStop()
  }

  @inline private def removeInternalObserverFromCurrentEventStream(): Unit = {
    maybeCurrentEventStream.foreach { currentStream =>
      Transaction.removeInternalObserver(currentStream, internalEventObserver)
    }
  }

}
