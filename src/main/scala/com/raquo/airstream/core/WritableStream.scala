package com.raquo.airstream.core

import com.raquo.airstream.core.AirstreamError.ObserverError

import scala.util.Try

trait WritableStream[A] extends EventStream[A] with WritableObservable[A] {

  override protected[this] def fireValue(nextValue: A, transaction: Transaction): Unit = {
    // println(s"$this > FIRE > $nextValue")

    // === CAUTION ===
    // The following logic must match Signal's fireTry! It is separated here for performance.

    isSafeToRemoveObserver = false

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

    isSafeToRemoveObserver = true

    maybePendingObserverRemovals.foreach { pendingObserverRemovals =>
      pendingObserverRemovals.forEach(remove => remove())
      pendingObserverRemovals.length = 0
    }
  }

  override protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    //println(s"$this > FIRE > $nextError")

    // === CAUTION ===
    // The following logic must match Signal's fireTry! It is separated here for performance.

    isSafeToRemoveObserver = false

    externalObservers.foreach { observer =>
      observer.onError(nextError)
    }

    internalObservers.foreach { observer =>
      InternalObserver.onError(observer, nextError, transaction)
    }

    isSafeToRemoveObserver = true

    maybePendingObserverRemovals.foreach { pendingObserverRemovals =>
      pendingObserverRemovals.forEach(remove => remove())
      pendingObserverRemovals.length = 0
    }
  }

  override protected[this] final def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    nextValue.fold(
      fireError(_, transaction),
      fireValue(_, transaction)
    )
  }

}
