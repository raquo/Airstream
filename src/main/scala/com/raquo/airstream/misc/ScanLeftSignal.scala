package com.raquo.airstream.misc

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Observable, Protected, Transaction}
import com.raquo.airstream.core.AirstreamError.InitialValueError
import com.raquo.airstream.util.tryOrFailure

import scala.scalajs.js
import scala.util.{Failure, Try}

/** @param makeInitialValue
  *     Note: Guarded against exceptions
  *     MUST RETURN SUCCESS IF `resumeOnError = true`, otherwise
  *     signal will be broken
  * @param fn
  *     Note: Guarded against exceptions
  * @param resumeOnError
  *     If true, [[fn]] will be called with "last seen success value"
  *     instead of "last seen value". In practice, if the user-facing
  *     `fn` is actually `(B, A) => B`, this lets you recover from
  *     exceptions in `fn` by using the previous (good) value of `B`.
  *
  *     If `resumeOnError` is true, you should ensure that the initial
  *     value is not an error. Otherwise, this signal will emit
  *     [[com.raquo.airstream.core.AirstreamError.InitialValueError]].
  *
  *     In contrast, if `resumeOnError` is false, then `(B, A) => B`
  *     will never be able to recover from the exception, keeping
  *     the signal in a failed state perpetually.
  */
class ScanLeftSignal[A, B, Parent <: Observable[A]](
  override protected[this] val parent: Parent,
  makeInitialValue: () => Try[B],
  fn: (Try[B], Try[A]) => Try[B],
  resumeOnError: Boolean
) extends SingleParentSignal[A, B] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  /** If `resumeOnError` is `false`, this is just a copy of [[maybeLastSeenCurrentValue]].
    *
    * If  `resumeOnError` is `true`, this becomes "last seen Success value", and
    * will be used by [[fn]] instead of [[maybeLastSeenCurrentValue]]. Except if the
    * initial value is a Failure (or throws), then this can still contain a Failure.
    */
  private var maybeEffectiveLastSeenCurrentValue: js.UndefOr[Try[B]] = js.undefined

  /** #Note: this is called from tryNow(), make sure to avoid infinite loop. */
  override protected def currentValueFromParent(): Try[B] = {
    parentAsSignalOpt.fold(
      ifEmpty = maybeLastSeenCurrentValue.getOrElse(tryOrFailure(makeInitialValue()))
    ) { parentSignal =>
      maybeLastSeenCurrentValue
        .map { lastSeenCurrentValue =>
          if (resumeOnError) {
            // #Safe When updating `maybeLastSeenCurrentValue`, we always write to
            //  `maybeEffectiveLastSeenCurrentValue` IF it's empty, so .get should be safe here.
            //  See `def setCurrentValue` below.
            val effectiveLastSeenValue = maybeEffectiveLastSeenCurrentValue.get
            // #Note if `resumeOnError` is true, `effectiveLastSeenValue` can contain an error
            //  if the error happened during evaluation of initial value. This is not supposed
            //  to happen in normal operation, so we throw it wrapped in InitialValueError.
            effectiveLastSeenValue.fold(
              err => Failure(InitialValueError(err)), // If we do get here, we'll likely get stuck.
              _ => tryOrFailure(fn(effectiveLastSeenValue, parentSignal.tryNow()))
            )
          } else {
            tryOrFailure(fn(lastSeenCurrentValue, parentSignal.tryNow()))
          }
        }
        .getOrElse(tryOrFailure(makeInitialValue()))
    }
  }

  override protected def setCurrentValue(newValue: Try[B]): Unit = {
    super.setCurrentValue(newValue)
    // Update `maybeEffectiveLastSeenCurrentValue` if ANY of the following cases:
    //  - if resumeOnError == false (then it just mirrors `maybeLastSeenCurrentValue`)
    //  - if newValue is a Success (as intended with resumeOnError = true)
    //  - if we're setting initial value now (`maybeEffectiveLastSeenCurrentValue` is empty)
    //     - this should not happen in normal operation, so we throw `InitialValueError` in this case.
    // Corollary:
    //  - `maybeLastSeenCurrentValue.isEmpty == maybeEffectiveLastSeenCurrentValue.isEmpty` is always true (outside of `setCurrentValue`).
    if (!resumeOnError || newValue.isSuccess || maybeEffectiveLastSeenCurrentValue.isEmpty) {
      maybeEffectiveLastSeenCurrentValue = newValue
    }
  }

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    super.onTry(nextParentValue, transaction)
    val currentValue = maybeEffectiveLastSeenCurrentValue.fold(
      tryNow()
    ) { effectiveLastSeenCurrentValue =>
      if (resumeOnError) {
        effectiveLastSeenCurrentValue.fold(
          err => Failure(InitialValueError(err)), // If we do get here, we'll likely get stuck.
          _ => effectiveLastSeenCurrentValue
        )
      } else {
        tryNow()
      }
    }
    fireTry(
      tryOrFailure(fn(currentValue, nextParentValue)),
      transaction
    )
  }
}
