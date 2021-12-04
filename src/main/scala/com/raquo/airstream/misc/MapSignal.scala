package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.AirstreamError.ErrorHandlingError
import com.raquo.airstream.core.{Protected, Signal, Transaction}

import scala.util.{Failure, Success, Try}

// @TODO[Elegance] Is this right that we shoved .recover into Map observables? Maybe it deserves its own class? But it's so many classes...

/** This signal emits an error if the parent observable emits an error or if `project` throws
  *
  * If `recover` is defined and needs to be called, it can do the following:
  * - Return Some(value) to make this signal emit value
  * - Return None to make this signal ignore (swallow) this error
  * - Not handle the error (meaning .isDefinedAt(error) must be false) to emit the original error
  *
  * @param project Note: guarded against exceptions
  * @param recover Note: guarded against exceptions
  */
class MapSignal[I, O](
  protected[this] val parent: Signal[I],
  protected[this] val project: I => O,
  protected[this] val recover: Option[PartialFunction[Throwable, Option[O]]]
) extends SingleParentSignal[I, O] with InternalTryObserver[I] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
    nextParentValue.fold(
      nextError => recover.fold(
        // if no `recover` specified, fire original error
        fireError(nextError, transaction)
      )(
        pf => Try(pf.applyOrElse(nextError, (_: Throwable) => null)).fold(
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

  override protected def currentValueFromParent(): Try[O] = {
    val originalValue = parent.tryNow().map(project)

    originalValue.fold(
      // if no `recover` specified, keep original error
      nextError => recover.fold(originalValue)(
        pf => Try(pf.applyOrElse(nextError, (_: Throwable) => null)).fold(
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
