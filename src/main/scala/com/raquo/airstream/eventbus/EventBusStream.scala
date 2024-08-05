package com.raquo.airstream.eventbus

import com.raquo.airstream.common.InternalNextErrorObserver
import com.raquo.airstream.core.{
  EventStream,
  Protected,
  Transaction,
  WritableStream
}
import com.raquo.ew.JsArray

class EventBusStream[A] private[eventbus] (
    parentDisplayName: => String
) extends WritableStream[A]
    with InternalNextErrorObserver[A] {

  private val sourceStreams: JsArray[EventStream[A]] = JsArray()

  /** Made more public to allow usage from WriteBus */
  override protected[eventbus] def isStarted: Boolean = super.isStarted

  override protected val topoRank: Int = 1

  @inline private[eventbus] def addSource(
      sourceStream: EventStream[A]
  ): Unit = {
    sourceStreams.push(sourceStream)
    if (isStarted) {
      sourceStream.addInternalObserver(this, shouldCallMaybeWillStart = true)
    }
  }

  private[eventbus] def removeSource(sourceStream: EventStream[A]): Unit = {
    val index = sourceStreams.indexOf(sourceStream)
    if (index != -1) {
      sourceStreams.splice(index, deleteCount = 1)
      if (isStarted) {
        sourceStream.removeInternalObserver(observer = this)
      }
    }
  }

  /** @param ignoredTransaction
    *   normally EventBus emits all events in a new transaction, so it ignores
    *   whatever is provided.
    */
  override protected def onNext(
      nextValue: A,
      ignoredTransaction: Transaction
  ): Unit = {
    // dom.console.log(s">>>>WBS.onNext($nextValue): isStarted=$isStarted")
    // dom.console.log(sources)

    // Note: We're not checking isStarted here because if this stream wasn't started, it wouldn't have been
    // fired as an internal observer. WriteBus calls this method manually, so it checks .isStarted on its own.
    // @TODO ^^^^ We should document this contract in InternalObserver

    // println(s"> init trx from EventBusStream(${nextValue})")

    Transaction(fireValue(nextValue, _))
  }

  /** Helper method to support batch emit using `WriteBus.emit` /
    * `WriteBus.emitTry`
    */
  private[eventbus] def onNextWithSharedTransaction(
      nextValue: A,
      sharedTransaction: Transaction
  ): Unit = {
    fireValue(nextValue, sharedTransaction)
  }

  /** Helper method to support batch emit using `WriteBus.emit` /
    * `WriteBus.emitTry`
    */
  private[eventbus] def onErrorWithSharedTransaction(
      nextError: Throwable,
      sharedTransaction: Transaction
  ): Unit = {
    fireError(nextError, sharedTransaction)
  }

  override protected def onError(
      nextError: Throwable,
      transaction: Transaction
  ): Unit = {
    Transaction(fireError(nextError, _))
  }

  override protected def onWillStart(): Unit = {
    sourceStreams.forEach(Protected.maybeWillStart(_))
  }

  override protected[this] def onStart(): Unit = {
    sourceStreams.forEach(
      _.addInternalObserver(this, shouldCallMaybeWillStart = false)
    )
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    // dom.console.log("EventBusStream STOPPED!", this.toString)
    sourceStreams.forEach(_.removeInternalObserver(observer = this))
    super.onStop()
  }

  override protected def defaultDisplayName: String =
    parentDisplayName + ".events"
}
