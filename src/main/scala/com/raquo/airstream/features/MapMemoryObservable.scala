package com.raquo.airstream.features

import com.raquo.airstream.core.{MemoryObservable, Transaction}
import com.raquo.airstream.core.AirstreamError.ErrorHandlingError

import scala.util.{Failure, Success, Try}

trait MapMemoryObservable[I, O] extends MemoryObservable[O] with SingleParentObservable[I, O] with InternalTryObserver[I] {

  /** Narrowing this down as a MemoryObservable */
  override protected[this] val parent: MemoryObservable[I]

  /** Note: guarded against exceptions */
  protected[this] val project: I => O

  /** Note: guarded against exceptions */
  protected[this] val recover: Option[PartialFunction[Throwable, Option[O]]]

  override protected[airstream] def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
    nextParentValue.fold(
      nextError => recover.fold(
        // if no `recover` specified, fire original error
        fireError(nextError, transaction))(
        pf => Try(pf.applyOrElse(nextError, null)).fold(
          tryError => {
            // if recover throws error, fire wrapped error
            fireError(ErrorHandlingError(error = tryError, cause = nextError), transaction)
          },
          nextValue => {
            if (nextValue == null) {
              // If recover was not applicable, fire original error
              fireError(nextError, transaction)
            } else {
              // If recover was applicable and resulted in a new value, fire that value
              nextValue.foreach(fireValue(_, transaction))
            }
          }
        )
      ),
      _ => fireTry(nextParentValue.map(project), transaction)
    )
  }

  override protected[this] def initialValue: Try[O] = {
    val originalValue = parent.tryNow().map(project)

    originalValue.fold(
      // if no `recover` specified, keep original error
      nextError => recover.fold(originalValue)(
        pf => Try(pf.applyOrElse(nextError, null)).fold(
          tryError => {
            // if recover throws error, save wrapped error
            Failure(ErrorHandlingError(error = tryError, cause = nextError))
          },
          nextValue => {
            if (nextValue == null) {
              // If recover was not applicable, keep original value
              originalValue
            } else {
              // @TODO[Test] Verify this
              // If recover was applicable and resulted in Some(value), use that.
              // If None, use the original error because we can't "skip" initial value
              nextValue.map(Success(_)).getOrElse(originalValue)
            }
          }
        )
      ),
      _ => originalValue)
  }
}
