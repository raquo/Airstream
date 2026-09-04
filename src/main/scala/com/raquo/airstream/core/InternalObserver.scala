package com.raquo.airstream.core

import scala.util.{Failure, Success, Try}

trait InternalObserver[-A] {

  /** Must not throw */
  protected def onNext(nextValue: A, transaction: Transaction): Unit

  /** Must not throw */
  protected def onError(nextError: Throwable, transaction: Transaction): Unit

  /** Must not throw */
  protected def onTry(nextValue: Try[A], transaction: Transaction): Unit
}

object InternalObserver {

  val empty: InternalObserver[Any] = new InternalObserver[Any] {

    override protected def onNext(nextValue: Any, transaction: Transaction): Unit = ()

    override protected def onError(nextError: Throwable, transaction: Transaction): Unit = ()

    override protected def onTry(nextValue: Try[Any], transaction: Transaction): Unit = ()
  }

  def apply[A](
    onNext: (A, Transaction) => Unit,
    onError: (Throwable, Transaction) => Unit
  ): InternalObserver[A] = {
    val onNextParam = onNext // It's beautiful on the outside
    val onErrorParam = onError

    new InternalObserver[A] {

      final override def onNext(nextValue: A, transaction: Transaction): Unit = {
        onNextParam(nextValue, transaction)
      }

      final override def onError(nextError: Throwable, transaction: Transaction): Unit = {
        onErrorParam(nextError, transaction)
      }

      final override def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
        nextValue.fold(onError(_, transaction), onNext(_, transaction))
      }
    }
  }

  def fromTry[A](onTry: (Try[A], Transaction) => Unit): InternalObserver[A] = {
    val onTryParam = onTry // It's beautiful on the outside

    new InternalObserver[A] {

      final override def onNext(nextValue: A, transaction: Transaction): Unit = {
        onTry(Success(nextValue), transaction)
      }

      final override def onError(nextError: Throwable, transaction: Transaction): Unit = {
        onTry(Failure(nextError), transaction)
      }

      final override def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
        onTryParam(nextValue, transaction)
      }
    }
  }

  @inline private[airstream] def onNext[A](
    observer: InternalObserver[A],
    nextValue: A,
    transaction: Transaction
  ): Unit = {
    observer.onNext(nextValue, transaction)
  }

  @inline private[airstream] def onError(
    observer: InternalObserver[?],
    nextError: Throwable,
    transaction: Transaction
  ): Unit = {
    observer.onError(nextError, transaction)
  }

  @inline private[airstream] def onTry[A](
    observer: InternalObserver[A],
    nextValue: Try[A],
    transaction: Transaction
  ): Unit = {
    observer.onTry(nextValue, transaction)
  }
}
