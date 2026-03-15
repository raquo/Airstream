package com.raquo.airstream.scan

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Observable, Protected, Signal, Transaction}

import scala.scalajs.js
import scala.util.Try

/**
 * Accumulates all emissions from the `parent` using a binary operator `combine`.
 * Forms a [[Signal]] that emits the accumulated value every time the `parent` emits.
 *
 * @param parent      The parent observable whose events or updates are accumulated.
 * @param makeInitial A generator for the accumulator's seed, given the initial value of the `parent`.
 * @param combine     A binary operator to update the accumulator given its previous value and the next event.
 *                    It is not safe to throw uncaught exceptions; you must use [[Try]] instead!
 * @param resetOnStop Whether to reset the accumulator when `parent` is restarted.
 * @tparam A          The type of values emitted by the `parent` observable.
 * @tparam B          The type of the accumulated value and thus of this signal.
 * @tparam Parent     The kind of observable on which this signal is based.
 */
class ScanLeftSignal[A, B, Parent <: Observable[A]] private[airstream] (
  override protected[this] val parent: Parent,
  makeInitial: () => Try[B],
  combine: (Try[B], Try[A]) => Try[B],
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
