package com.raquo.airstream.misc

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Protected, Signal, Transaction}
import com.raquo.airstream.core.AirstreamError.{ErrorHandlingError, InitialValueError}

import scala.util.{Failure, Success, Try}

// @TODO[Elegance] Is this right that we shoved .recover into Map observables? Maybe it deserves its own class? But it's so many classes...

/** This signal emits an error if the parent observable emits an error or if `project` throws
  *
  * If `recover` is defined and needs to be called, it can do the following:
  *  - Return Some(value) to make this signal emit value
  *  - Return None to make this signal ignore (swallow) this error
  *    - #Warning: Except if this error is the Signal's initial value, then we'll use it anyway.
  *  - Not handle the error (meaning .isDefinedAt(error) must be false) to emit the original error
  *
  * @param project Note: guarded against exceptions
  * @param recover Note: guarded against exceptions
  */
class MapSignal[I, O](
  protected[this] val parent: Signal[I],
  protected[this] val project: I => O,
  protected[this] val recover: Option[PartialFunction[Throwable, Option[O]]]
) extends SingleParentSignal[I, O] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
    super.onTry(nextParentValue, transaction)
    nextParentValue.fold(
      nextError =>
        recover.fold(
          // if no `recover` specified, fire original error
          fireError(nextError, transaction)
        ) { pf =>
          Try(pf.lift(nextError)).fold(
            pfError => {
              // if recover throws error, fire wrapped error
              fireError(ErrorHandlingError(error = pfError, cause = nextError), transaction)
            },
            _.fold({
              // If recover was not applicable, fire original error
              fireError(nextError, transaction)
            }) { nextValueOpt =>
              // If recover was applicable and resulted in a new value, fire that value
              nextValueOpt.foreach(fireValue(_, transaction))
            }
          )
        },
      _ => fireTry(nextParentValue.map(project), transaction)
    )
  }

  override protected def currentValueFromParent(): Try[O] = {
    val originalValue = parent.tryNow().map(project)

    originalValue.fold(
      // if no `recover` specified, keep original error
      nextError =>
        recover.fold(
          // If no `recover` specified, use original error
          originalValue
        ) { pf =>
          Try(pf.lift(nextError)).fold(
            pfError => {
              // if recover throws error, save wrapped error
              Failure(ErrorHandlingError(error = pfError, cause = nextError))
            },
            _.fold({
              // If recover was not applicable, keep original value
              originalValue
            }) { nextValueOpt =>
              // @TODO[Test] Verify this
              // If recover was applicable and resulted in Some(value), use that.
              // If None, we want to keep the signal's previous value (maybeLastSeenCurrentValue),
              // but if that's not available (because we're currently evaluating Signal's initial value),
              // then we're forced to use the original error because we can't "skip" initial value.
              // #TODO[Integrity,API] – this is like having `filter` on signals, but in the error channel
              nextValueOpt.fold(
                maybeLastSeenCurrentValue.getOrElse(Failure(InitialValueError(error = nextError)))
              )(
                Success(_)
              )
            }
          )
        },
      _ => originalValue
    )
  }
}
