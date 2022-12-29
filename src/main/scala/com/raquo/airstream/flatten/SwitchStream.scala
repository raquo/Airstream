package com.raquo.airstream.flatten

import com.raquo.airstream.common.InternalNextErrorObserver
import com.raquo.airstream.core.{EventStream, InternalObserver, Observable, Protected, Signal, Transaction, WritableStream}

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
class SwitchStream[I, O](
  parent: Observable[I],
  makeStream: I => EventStream[O]
) extends WritableStream[O] with InternalNextErrorObserver[I] {

  override protected val topoRank: Int = 1

  private[this] val parentIsSignal: Boolean = parent.isInstanceOf[Signal[_]]

  private[this] var maybeCurrentEventStreamTry: js.UndefOr[Try[EventStream[O]]] = js.undefined

  private[this] var maybeNextEventStreamTry: js.UndefOr[Try[EventStream[O]]] = js.undefined

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
    switchToNextStream(nextStream = makeStream(nextValue), isStarting = false)
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    switchToNextError(nextError, Some(transaction))
  }

  override protected def onWillStart(): Unit = {
    Protected.maybeWillStart(parent)
    if (parentIsSignal) {
      val parentSignal = parent.asInstanceOf[Signal[I @unchecked]]
      val newStreamTry = parentSignal.tryNow().map(makeStream)
      newStreamTry.foreach(Protected.maybeWillStart)
      maybeNextEventStreamTry = newStreamTry
    } else {
      maybeCurrentEventStreamTry.foreach(_.foreach(Protected.maybeWillStart))
    }
  }

  override protected[this] def onStart(): Unit = {
    parent.addInternalObserver(this, shouldCallMaybeWillStart = false)

    if (parentIsSignal) {
      maybeNextEventStreamTry.foreach {
        case Success(nextStream) =>
          switchToNextStream(nextStream, isStarting = true)
        case Failure(nextError) =>
          switchToNextError(nextError, transaction = None)
      }
      maybeNextEventStreamTry = js.undefined
    } else {
      maybeCurrentEventStreamTry.foreach { _.foreach { currentStream =>
        currentStream.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = false)
      }}
    }

    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    parent.removeInternalObserver(observer = this)
    removeInternalObserverFromCurrentEventStream()
    super.onStop()
  }

  private def switchToNextStream(nextStream: EventStream[O], isStarting: Boolean): Unit = {
    val isSameStream = maybeCurrentEventStreamTry.exists { currentStream =>
      currentStream.isSuccess && (currentStream.get eq nextStream)
    }
    if (!isSameStream) {
      removeInternalObserverFromCurrentEventStream()
      maybeCurrentEventStreamTry = Success(nextStream)
    }

    if (!isSameStream || isStarting) {
      nextStream.addInternalObserver(internalEventObserver, shouldCallMaybeWillStart = !isStarting)
    }
  }

  private def switchToNextError(nextError: Throwable, transaction: Option[Transaction]): Unit = {
    removeInternalObserverFromCurrentEventStream()
    maybeCurrentEventStreamTry = Failure(nextError)
    transaction.fold[Unit](new Transaction(fireError(nextError, _)))(fireError(nextError, _)) // #Note[onStart,trx,loop]
  }

  private def removeInternalObserverFromCurrentEventStream(): Unit = {
    maybeCurrentEventStreamTry.foreach { _.foreach { currentStream =>
      currentStream.removeInternalObserver(internalEventObserver)
    }}
  }

}
