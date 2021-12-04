package com.raquo.airstream.core

import com.raquo.airstream.core.AirstreamError.ObserverError

import scala.util.Try

trait WritableEventStream[A] extends EventStream[A] with WritableObservable[A] {

  override protected[this] def fireValue(nextValue: A, transaction: Transaction): Unit = {
    //println(s"$this > FIRE > $nextValue")
    // Note: Removal of observers is always done at the end of a transaction, so the iteration here is safe

    // === CAUTION ===
    // The following logic must match Signal's fireTry! It is separated here for performance.

    externalObservers.foreach { observer =>
      try {
        observer.onNext(nextValue)
      } catch {
        case err: Throwable => AirstreamError.sendUnhandledError(ObserverError(err))
      }
    }

    internalObservers.foreach { observer =>
      InternalObserver.onNext(observer, nextValue, transaction)
    }
  }

  override protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    //println(s"$this > FIRE > $nextError")
    // Note: Removal of observers is always done at the end of a transaction, so the iteration here is safe

    // === CAUTION ===
    // The following logic must match Signal's fireTry! It is separated here for performance.

    externalObservers.foreach { observer =>
      observer.onError(nextError)
    }

    internalObservers.foreach { observer =>
      InternalObserver.onError(observer, nextError, transaction)
    }
  }

  override protected[this] final def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    nextValue.fold(
      fireError(_, transaction),
      fireValue(_, transaction)
    )
  }

}
