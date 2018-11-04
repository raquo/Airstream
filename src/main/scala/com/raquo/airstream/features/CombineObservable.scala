package com.raquo.airstream.features

import com.raquo.airstream.core.{InternalObserver, SyncObservable, Transaction}
import com.raquo.airstream.core.AirstreamError.CombinedError

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

trait CombineObservable[A] extends SyncObservable[A] { self =>

  protected[this] var maybeCombinedValue: Option[Try[A]] = None

  /** Parent observers are not immediately active. onStart/onStop regulates that. */
  protected[this] val parentObservers: js.Array[InternalParentObserver[_]] = js.Array()

  // Implementations should call internalObserver.onNext() instead of .fire()
  // Transaction will call .fireSync() when it's time, and that will in turn call .fire()
  protected[this] val internalObserver: InternalObserver[A] = InternalObserver.fromTry[A]((nextValue, transaction) => {
    if (!transaction.pendingObservables.contains(self)) {
      // println(s"Marking CombineObs($id) as pending in TRX(${transaction.id})")
      transaction.pendingObservables.enqueue(self)
    }
    maybeCombinedValue = Some(nextValue)
  })

  /** This method is called after this pending observable has been resolved */
  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    maybeCombinedValue.foreach { combinedValue =>
      maybeCombinedValue = None
      fireTry(combinedValue, transaction)
    }
  }

  override protected[this] def onStart(): Unit = {
    parentObservers.foreach(_.addToParent())
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    parentObservers.foreach(_.removeFromParent())
    super.onStop()
  }

}

object CombineObservable {

  // @TODO[Performance] There's probably a better way to do this
  def guardedCombinator[A, B, O](combinator: (A, B) => O)(try1: Try[A], try2: Try[B]): Try[O] = {
    if (try1.isSuccess && try2.isSuccess) {
      Success(combinator(try1.get, try2.get))
    } else {
      Failure(CombinedError(List(try1.toEither.left.toOption, try2.toEither.left.toOption)))
    }
  }
}
