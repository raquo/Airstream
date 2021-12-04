package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentEventStream}
import com.raquo.airstream.core.AirstreamError.ErrorHandlingError
import com.raquo.airstream.core.{Observable, Protected, Transaction}

import scala.util.Try

/** This stream applies a `project` function to events fired by its parent and fires the resulting value
  *
  * This stream emits an error if the parent observable emits an error or if `project` throws
  *
  * If `recover` is defined and needs to be called, it can do the following:
  * - Return Some(value) to make this stream emit value
  * - Return None to make this stream ignore (swallow) this error
  * - Not handle the error (meaning .isDefinedAt(error) must be false) to emit the original error
  *
  * If `recover` throws an exception, it will be wrapped in `ErrorHandlingError` and propagated.
  *
  * @param project Note: guarded against exceptions
  * @param recover Note: guarded against exceptions
  */
class MapEventStream[I, O](
  override protected[this] val parent: Observable[I],
  project: I => O,
  recover: Option[PartialFunction[Throwable, Option[O]]]
) extends SingleParentEventStream[I, O] with InternalNextErrorObserver[I] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onNext(nextParentValue: I, transaction: Transaction): Unit = {
    Try(project(nextParentValue)).fold(
      onError(_, transaction),
      fireValue(_, transaction)
    )
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    recover.fold(
      // if no `recover` specified, fire original error
      fireError(nextError, transaction))(
      pf => Try(pf.applyOrElse(nextError, (_: Throwable) => null)).fold(
        tryError => {
          // if recover throws error, fire a wrapped error
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
    )
  }
}
