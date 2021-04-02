package com.raquo.airstream.eventbus

import com.raquo.airstream.core.{EventStream, Observer, Transaction}
import com.raquo.airstream.ownership.{Owner, Subscription}
import com.raquo.airstream.util.hasDuplicateTupleKeys

import scala.util.Try

class Writer[A] extends Observer[A] {

  /** Hidden here because the public interface of Writer is all about writing
    * rather than reading, but exposed in [[EventBus]]
    */
  private[eventbus] val stream: EventBusStream[A] = new EventBusStream()

  /** Note: this source will be removed when the `owner` you provide says so.
    * To remove this source manually, call .kill() on the resulting Subscription.
    */
  def addSource(sourceStream: EventStream[A])(implicit owner: Owner): Subscription = {
    stream.addSource(sourceStream)
    new Subscription(owner, cleanup = () => {
      stream.removeSource(sourceStream)
    })
  }

  def contracomposeWriter[B](operator: EventStream[B] => EventStream[A])(implicit owner: Owner): Writer[B] = {
    val mapBus = new Writer[B]
    addSource(mapBus.stream.compose(operator))(owner)
    mapBus
  }

  /** Behaves similar to `contramap`, but gives you a Writer, not just an Observer */
  def contramapWriter[B](project: B => A)(implicit owner: Owner): Writer[B] = {
    contracomposeWriter[B](_.map(project))(owner)
  }

  /** Behaves similar to `filter`, but gives you a Writer, not just an Observer */
  def filterWriter(passes: A => Boolean)(implicit owner: Owner): Writer[A] = {
    val filterBus = new Writer[A]
    addSource(filterBus.stream.filter(passes))(owner)
    filterBus
  }

  override def onNext(nextValue: A): Unit = {
    if (stream.isStarted) { // important check
      // @TODO[Integrity] We rely on the knowledge that EventBusStream discards the transaction it's given. Laaaame
      stream.onNext(nextValue, ignoredTransaction = null)
    }
    // else {
    //   println(">>>> Writer.onNext called, but stream is not started!")
    // }
  }

  override def onError(nextError: Throwable): Unit = {
    if (stream.isStarted) {
      // @TODO[Integrity] We rely on the knowledge that EventBusStream discards the transaction it's given. Laaaame
      stream.onError(nextError, transaction = null)
    }
  }

  override final def onTry(nextValue: Try[A]): Unit = {
    nextValue.fold(onError, onNext)
  }

  private[eventbus] def onNextWithSharedTransaction(nextValue: A, sharedTransaction: Transaction): Unit = {
    if (stream.isStarted) {
      stream.onNextWithSharedTransaction(nextValue, sharedTransaction)
    }
  }

  private[eventbus] def onErrorWithSharedTransaction(nextError: Throwable, sharedTransaction: Transaction): Unit = {
    if (stream.isStarted) {
      stream.onErrorWithSharedTransaction(nextError, sharedTransaction)
    }
  }

  private[eventbus] def onTryWithSharedTransaction(nextValue: Try[A], sharedTransaction: Transaction): Unit = {
    nextValue.fold(
      onErrorWithSharedTransaction(_, sharedTransaction),
      onNextWithSharedTransaction(_, sharedTransaction)
    )
  }
}

object Writer {

  type WriterTuple[A] = (Writer[A], A)

  type WriterTryTuple[A] = (Writer[A], Try[A])

  /** Emit events into several Writers at once (in the same transaction)
    * Example usage: emitTry(writer1 -> value1, writer2 -> value2)
    */
  def emit[A](values: WriterTuple[A]*): Unit = {
    //println(s"> init trx from Writer.emit($values)")
    if (hasDuplicateTupleKeys(values)) {
      throw new Exception("Unable to {EventBus,Writer}.emit: the provided list of event buses has duplicates. You can't make an observable emit more than one event per transaction.")
    }
    new Transaction(trx => values.foreach(emitValue(_, trx)))
  }

  /** Emit events into several Writers at once (in the same transaction)
    * Example usage: emitTry(writer1 -> Success(value1), writer2 -> Failure(error2))
    */
  def emitTry[A](values: WriterTryTuple[A]*): Unit = {
    //println(s"> init trx from Writer.emitTry($values)")
    if (hasDuplicateTupleKeys(values)) {
      throw new Exception("Unable to {EventBus,Writer}.emitTry: the provided list of event buses has duplicates. You can't make an observable emit more than one event per transaction.")
    }
    new Transaction(trx => values.foreach(emitTryValue(_, trx)))
  }

  @inline private def emitValue[A](tuple: WriterTuple[A], transaction: Transaction): Unit = {
    tuple._1.onNextWithSharedTransaction(tuple._2, transaction)
  }

  @inline private def emitTryValue[A](tuple: WriterTryTuple[A], transaction: Transaction): Unit = {
    tuple._1.onTryWithSharedTransaction(tuple._2, transaction)
  }
}
