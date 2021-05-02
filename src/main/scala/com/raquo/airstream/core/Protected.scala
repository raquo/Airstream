package com.raquo.airstream.core

import scala.annotation.{implicitNotFound, unused}
import scala.util.Try

@implicitNotFound("Implicit instance of Airstream's `Protected` class not found. You're trying to access a method which is designed to only be accessed from inside a BaseObservable subtype.")
class Protected private ()

object Protected {

  private[airstream] implicit val protectedAccessEvidence: Protected = new Protected()

  @inline def topoRank[O[+_] <: Observable[_]](observable: BaseObservable[O, _]): Int = {
    BaseObservable.topoRank(observable)
  }

  def maxParentTopoRank[O[+_] <: Observable[_]](parents: Iterable[BaseObservable[O, _]]): Int = {
    parents.foldLeft(0)((maxRank, parent) => Protected.topoRank(parent) max maxRank)
  }

  @inline def tryNow[A](signal: Signal[A])(implicit @unused ev: Protected): Try[A] = signal.tryNow()

  @inline def now[A](signal: Signal[A])(implicit @unused ev: Protected): A = signal.now()

  @inline def onNext[A](
    observer: InternalObserver[A],
    nextValue: A,
    transaction: Transaction
  )(
    implicit @unused ev: Protected
  ): Unit = {
    InternalObserver.onNext(observer, nextValue, transaction)
  }

  @inline def onError(
    observer: InternalObserver[_],
    nextError: Throwable,
    transaction: Transaction
  )(
    implicit @unused ev: Protected
  ): Unit = {
    InternalObserver.onError(observer, nextError, transaction)
  }

  @inline def onTry[A](
    observer: InternalObserver[A],
    nextValue: Try[A],
    transaction: Transaction
  )(
    implicit @unused ev: Protected
  ): Unit = {
    InternalObserver.onTry(observer, nextValue, transaction)
  }
}
