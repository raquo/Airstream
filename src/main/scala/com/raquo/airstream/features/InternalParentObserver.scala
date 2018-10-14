package com.raquo.airstream.features

import com.raquo.airstream.core.{InternalObserver, Observable, Transaction}

import scala.util.Try

trait InternalParentObserver[A] extends InternalObserver[A] {

  protected[this] val parent: Observable[A]

  def addToParent(): Unit = {
    parent.addInternalObserver(this)
  }

  def removeFromParent(): Unit = {
    Transaction.removeInternalObserver(parent, observer = this)
  }
}

object InternalParentObserver {

  protected[airstream] def apply[A](
    parent: Observable[A],
    onNext: (A, Transaction) => Unit,
    onError: (Throwable, Transaction) => Unit
  ): InternalParentObserver[A] = {
    val parentParam = parent
    val onNextParam = onNext
    val onErrorParam = onError
    new InternalParentObserver[A] with InternalNextErrorObserver[A] {

      override protected[this] val parent: Observable[A] = parentParam

      override protected[airstream] final def onNext(nextValue: A, transaction: Transaction): Unit = {
        onNextParam(nextValue, transaction)
      }

      override protected[airstream] final def onError(nextError: Throwable, transaction: Transaction): Unit = {
        onErrorParam(nextError, transaction)
      }
    }
  }

  protected[airstream] def fromTry[A](
    parent: Observable[A],
    onTry: (Try[A], Transaction) => Unit
  ): InternalParentObserver[A] = {
    val parentParam = parent
    val onTryParam = onTry
    new InternalParentObserver[A] with InternalTryObserver[A] {

      override protected[this] val parent: Observable[A] = parentParam

      override protected[airstream] final def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
        onTryParam(nextValue, transaction)
      }
    }
  }
}
