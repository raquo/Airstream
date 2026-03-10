package com.raquo.airstream.scan

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Observable, Protected, Signal, Transaction}
import com.raquo.airstream.scan.Recover.CombineTry

import scala.scalajs.js
import scala.util.Try

/**
 * Accumulates all emissions from the `parent` using a binary operator `combine`.
 * Forms a [[com.raquo.airstream.core.Signal]] that emits the accumulated value every time the `parent` emits.
 * 
 * @param parent      The parent observable whose emissions are combined with `combine`.
 * @param makeInitial A function for creating the seed value for this accumulator given the initial value of the `parent`.
 * @param combine     A binary operator that takes a tuple of the previously accumulated value and
 *                    the next emission from the `parent` to produce the next accumulated value.
 *                    It is not safe for `combine` to throw uncaught exceptions; you must use [[Try]] instead!
 * @param resetOnStop Whether to reset the accumulator when the `parent` is restarted (default is `false`).
 * @tparam A          The type of values emitted by the `parent` observable.
 * @tparam B          The type of the accumulated value and thus of this signal.
 * @tparam Parent     The kind of observable on which this signal is based.
 *
 * @note Should be constructed using the operators in [[ScanLeftOps]] such as `scanLeft`.
 */
class ScanLeftSignal[A, B, Parent <: Observable[A]] private[airstream] (
  override protected[this] val parent: Parent,
  makeInitial: () => Try[B],
  combine: CombineTry[A, B],
  resetOnStop: Boolean = false,
) extends SingleParentSignal[A, B] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  /** #Note: this is called from tryNow(), make sure to avoid infinite loop. */
  override protected def currentValueFromParent(): Try[B] = {
    if (resetOnStop) {
      makeInitial()
    } else {
      parentAsSignalOpt.fold(
        ifEmpty = maybeLastSeenCurrentValue.getOrElse(makeInitial())
      ) { parentSignal =>
        maybeLastSeenCurrentValue
          .map(lastSeenCurrentValue => combine(lastSeenCurrentValue, parentSignal.tryNow()))
          .getOrElse(makeInitial())
      }
    }
  }

  override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
    super.onTry(nextParentValue, transaction)
    fireTry(combine(tryNow(), nextParentValue), transaction)
  }

  override protected[this] def onStop(): Unit = {
    if (resetOnStop) {
      // Clear the cached value and advance the update ID so that:
      //  - The next tryNow() call re-evaluates from makeInitial(). This is required for
      //    stream-backed signals because onWillStart() never calls currentValueFromParent()
      //    (streams have no "current value" to peek at). For signal-backed signals it also
      //    handles the case where the parent has NOT updated while we were stopped.
      //  - Downstream signals (e.g. from a chained .map()) detect the state change via
      //    peekWhetherParentHasUpdated() and re-derive their cached values on restart.
      _lastUpdateId = Signal.nextUpdateId()
      maybeLastSeenCurrentValue = js.undefined
    }
    super.onStop()
  }
}
