package com.raquo.airstream.eventbus

import com.raquo.airstream.core.{EventStream, InternalObserver, Observer, Transaction}
import com.raquo.airstream.ownership.{Owner, Subscription}
import com.raquo.airstream.util.hasDuplicateTupleKeys

import scala.util.Try

class WriteBus[A] extends Observer[A] {

  /** Hidden here because the public interface of WriteBus is all about writing
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

  def contracomposeWriter[B](operator: EventStream[B] => EventStream[A])(implicit owner: Owner): WriteBus[B] = {
    val mapBus = new WriteBus[B]
    addSource(mapBus.stream.compose(operator))(owner)
    mapBus
  }

  /** Behaves similar to `contramap`, but gives you a WriteBus, not just an Observer */
  def contramapWriter[B](project: B => A)(implicit owner: Owner): WriteBus[B] = {
    contracomposeWriter[B](_.map(project))(owner)
  }

  /** Behaves similar to `filter`, but gives you a WriteBus, not just an Observer */
  def filterWriter(passes: A => Boolean)(implicit owner: Owner): WriteBus[A] = {
    val filterBus = new WriteBus[A]
    addSource(filterBus.stream.filter(passes))(owner)
    filterBus
  }

  override def onNext(nextValue: A): Unit = {
    if (stream.isStarted) { // important check
      // @TODO[Integrity] We rely on the knowledge that EventBusStream discards the transaction it's given. Laaaame
      InternalObserver.onNext(stream, nextValue, transaction = null)
    }
    // else {
    //   println(">>>> WriteBus.onNext called, but stream is not started!")
    // }
  }

  override def onError(nextError: Throwable): Unit = {
    if (stream.isStarted) {
      // @TODO[Integrity] We rely on the knowledge that EventBusStream discards the transaction it's given. Laaaame
      InternalObserver.onError(stream, nextError, transaction = null)
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

object WriteBus {

  implicit class BusTuple[A](val tuple: (WriteBus[A], A)) extends AnyVal

  implicit class BusTryTuple[A](val tuple: (WriteBus[A], Try[A])) extends AnyVal

  /** Emit events into several WriteBus-es at once (in the same transaction)
    * Example usage: emitTry(writeBus1 -> value1, writeBus2 -> value2)
    */
  def emit(values: BusTuple[_]*): Unit = {
    //println(s"> init trx from WriteBus.emit($values)")
    if (hasDuplicateTupleKeys(values.map(_.tuple))) {
      throw new Exception("Unable to {EventBus,WriteBus}.emit: the provided list of event buses has duplicates. You can't make an observable emit more than one event per transaction.")
    }
    new Transaction(trx => values.foreach(emitValue(_, trx)))
  }

  /** Emit events into several WriteBus-es at once (in the same transaction)
    * Example usage: emitTry(writeBus1 -> Success(value1), writeBus2 -> Failure(error2))
    */
  def emitTry(values: BusTryTuple[_]*): Unit = {
    //println(s"> init trx from WriteBus.emitTry($values)")
    if (hasDuplicateTupleKeys(values.map(_.tuple))) {
      throw new Exception("Unable to {EventBus,WriteBus}.emitTry: the provided list of event buses has duplicates. You can't make an observable emit more than one event per transaction.")
    }
    new Transaction(trx => values.foreach(emitTryValue(_, trx)))
  }

  @inline private def emitValue[A](tuple: BusTuple[A], transaction: Transaction): Unit = {
    tuple.tuple._1.onNextWithSharedTransaction(tuple.tuple._2, transaction)
  }

  @inline private def emitTryValue[A](tuple: BusTryTuple[A], transaction: Transaction): Unit = {
    tuple.tuple._1.onTryWithSharedTransaction(tuple.tuple._2, transaction)
  }
}
