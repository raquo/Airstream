package com.raquo.airstream.flatten

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentEventStream}
import com.raquo.airstream.core.{EventStream, InternalObserver, Observable, Protected, Signal, Transaction}

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** `parent` observable emits values that we convert into streams using `makeStream`.
  *
  * This stream emits the events from the last such stream created this way.
  *
  * Events are emitted at the same time as the currently tracked stream emits them (but in a new transaction).
  *
  * When `parent` emits a nextValue, this stream switches to emitting events from `makeStream(nextValue)` (which is a stream).
  *
  * If parent stream emits an error, this stream re-emits that error and unsubscribes from the last emitted stream
  *
  * If the stream created with makeStream emits an error, this stream re-emits it in a new transaction.
  *
  * If parent is a signal in a failed state when SwitchEventStream is created, parent's error is re-emitted in a new
  * transaction, as if makeStream returned a stream that emitted this error.
  *
  * Warning: Similar to [[com.raquo.airstream.eventbus.EventBus]], this stream emits events in
  * a new transaction because its proper topoRank would need to be dynamic, which we don't support.
  *
  * @param makeStream Note: Must not throw
  */
class SwitchEventStream[I, O](
  override protected[this] val parent: Observable[I],
  makeStream: I => EventStream[O]
) extends SingleParentEventStream[I, O] with InternalNextErrorObserver[I] {

  override protected val topoRank: Int = 1

  private[this] val parentIsSignal: Boolean = parent.isInstanceOf[Signal[_]]

  private[this] var maybeCurrentEventStream: js.UndefOr[Try[EventStream[O]]] = js.undefined

  private[this] var maybeNextEventStream: js.UndefOr[Try[EventStream[O]]] = js.undefined

  // @TODO[Elegance] Maybe we should abstract away this kind of internal observer
  private[this] val internalEventObserver: InternalObserver[O] = InternalObserver[O](
    onNext = (nextEvent, _) => {
      //println(s"> init trx from SwitchEventStream.onValue(${nextEvent})")
      new Transaction(fireValue(nextEvent, _))
    },
    onError = (nextError, _) => {
      new Transaction(fireError(nextError, _))
    }
  )

  override protected def onNext(nextValue: I, transaction: Transaction): Unit = {
    switchToNextStream(nextStream = makeStream(nextValue), shouldCallMaybeWillStart = true)
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    switchToNextError(nextError, Some(transaction))
  }

  override protected def onWillStart(): Unit = {
    super.onWillStart() // start parent

    if (parentIsSignal) {
      val parentSignal = parent.asInstanceOf[Signal[I @unchecked]]
      val newStream = parentSignal.tryNow().map(makeStream)
      newStream.foreach(Protected.maybeWillStart)
      maybeNextEventStream = newStream
    } else {
      maybeCurrentEventStream.foreach(_.foreach(Protected.maybeWillStart))
    }
  }

  override protected[this] def onStart(): Unit = {
    super.onStart()

    maybeNextEventStream.foreach {
      case Success(nextStream) =>
        switchToNextStream(nextStream, shouldCallMaybeWillStart = false)
      case Failure(nextError) =>
        switchToNextError(nextError, transaction = None)
    }
    maybeNextEventStream = js.undefined
  }

  override protected[this] def onStop(): Unit = {
    removeInternalObserverFromCurrentEventStream()
    maybeCurrentEventStream = js.undefined
    super.onStop()
  }

  private def switchToNextStream(nextStream: EventStream[O], shouldCallMaybeWillStart: Boolean): Unit = {
    val isSameStream = maybeCurrentEventStream.exists { currentStream =>
      currentStream.isSuccess && (currentStream.get == nextStream)
    }
    if (!isSameStream) {
      removeInternalObserverFromCurrentEventStream()
      maybeCurrentEventStream = Success(nextStream)
      // If we're receiving events, this stream is started, so no need to check for that
      nextStream.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = shouldCallMaybeWillStart)
    }
  }

  private def switchToNextError(nextError: Throwable, transaction: Option[Transaction]): Unit = {
    removeInternalObserverFromCurrentEventStream()
    maybeCurrentEventStream = Failure(nextError)
    transaction.fold[Unit](new Transaction(fireError(nextError, _)))(fireError(nextError, _)) // #Note[onStart,trx,loop]
  }

  private def removeInternalObserverFromCurrentEventStream(): Unit = {
    maybeCurrentEventStream.foreach { _.foreach { currentStream =>
      Transaction.removeInternalObserver(currentStream, internalEventObserver)
    }}
  }

}
