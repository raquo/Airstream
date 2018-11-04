package com.raquo.airstream.features

import com.raquo.airstream.core.{InternalObserver, Transaction}

import scala.util.{Failure, Success}

trait InternalTryObserver[A] extends InternalObserver[A] {

  override protected[airstream] final def onNext(nextValue: A, transaction: Transaction): Unit = {
    onTry(Success(nextValue), transaction)
  }

  override protected[airstream] final def onError(nextError: Throwable, transaction: Transaction): Unit = {
    onTry(Failure(nextError), transaction)
  }
}
