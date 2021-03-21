package com.raquo.airstream.core

import com.raquo.airstream.core.AirstreamError.ObserverError

import scala.util.Try

trait WritableEventStream[A] extends WritableObservable[A] with EventStreamOps[A] {

  override protected[this] def fireValue(nextValue: A, transaction: Transaction): Unit = {
    // Note: Removal of observers is always done at the end of a transaction, so the iteration here is safe

    // === CAUTION ===
    // The following logic must match Signal's fireTry! It is separated here for performance.

    ObserverRegistry.getExternalObservers(this).foreach { observer =>
      try {
        observer.onNext(nextValue)
      } catch {
        case err: Throwable => AirstreamError.sendUnhandledError(ObserverError(err))
      }
    }

    ObserverRegistry.getInternalObservers(this).foreach { observer =>
      observer.onNext(nextValue, transaction)
    }
  }

  override protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    // Note: Removal of observers is always done at the end of a transaction, so the iteration here is safe

    // === CAUTION ===
    // The following logic must match Signal's fireTry! It is separated here for performance.

    ObserverRegistry.getExternalObservers(this).foreach { observer =>
      observer.onError(nextError)
    }

    ObserverRegistry.getInternalObservers(this).foreach { observer =>
      observer.onError(nextError, transaction)
    }
  }

  override protected[this] final def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    nextValue.fold(
      fireError(_, transaction),
      fireValue(_, transaction)
    )
  }


}
