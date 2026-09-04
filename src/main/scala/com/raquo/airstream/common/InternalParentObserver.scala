package com.raquo.airstream.common

import com.raquo.airstream.core.{InternalObserver, Observable, Transaction}

import scala.util.Try

trait InternalParentObserver[A] extends InternalObserver[A] {

  protected val parent: Observable[A]

  def addToParent(shouldCallMaybeWillStart: Boolean): Unit = {
    parent.addInternalObserver(this, shouldCallMaybeWillStart)
  }

  def removeFromParent(): Unit = {
    parent.removeInternalObserver(observer = this)
  }
}

object InternalParentObserver {

  def apply[A](
    parent: Observable[A],
    onNext: (A, Transaction) => Unit,
    onError: (Throwable, Transaction) => Unit
  ): InternalParentObserver[A] = {
    val parentParam = parent
    val onNextParam = onNext
    val onErrorParam = onError
    new InternalParentObserver[A] with InternalNextErrorObserver[A] {

      override protected val parent: Observable[A] = parentParam

      final override protected def onNext(nextValue: A, transaction: Transaction): Unit = {
        onNextParam(nextValue, transaction)
      }

      final override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
        onErrorParam(nextError, transaction)
      }
    }
  }

  def fromTry[A](
    parent: Observable[A],
    onTry: (Try[A], Transaction) => Unit
  ): InternalParentObserver[A] = {
    val parentParam = parent
    val onTryParam = onTry
    new InternalParentObserver[A] with InternalTryObserver[A] {

      override protected val parent: Observable[A] = parentParam

      final override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
        onTryParam(nextValue, transaction)
      }
    }
  }
}
