package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentStream}
import com.raquo.airstream.core.{EventStream, Observable, Protected, Signal, Transaction}
import com.raquo.ew.JsArray

import scala.scalajs.js
import scala.util.Try

/** This stream emits the combined value when samplingStreams emits.
  *
  * When the combined stream emits, it looks up the current value of sampledSignals,
  * but updates to those signals do not trigger updates to the combined stream.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param sampledSignals Never update this array - this stream owns it.
  * @param combinator Note: Must not throw!
  */
class SampleCombineStreamN[A, Out](
  samplingStream: EventStream[A],
  sampledSignals: JsArray[Signal[A]],
  combinator: JsArray[A] => Out
) extends MultiParentStream[A, Out] with CombineObservable[Out] {

  override protected val topoRank: Int = Protected.maxTopoRank(samplingStream, sampledSignals) + 1

  private var maybeLastSamplingValue: js.UndefOr[Try[A]] = js.undefined

  override protected def inputsReady: Boolean = maybeLastSamplingValue.nonEmpty

  override protected val parents: JsArray[Observable[A]] = {
    val arr = JsArray[Observable[A]](samplingStream)
    sampledSignals.forEach { sampledSignal =>
      arr.push(sampledSignal)
    }
    arr
  }

  override protected val parentObservers: JsArray[InternalParentObserver[?]] = {
    val arr = JsArray[InternalParentObserver[?]](
      InternalParentObserver.fromTry[A](
        samplingStream,
        (nextSamplingValue, trx) => {
          maybeLastSamplingValue = nextSamplingValue
          onInputsReady(trx)
        }
      )
    )
    sampledSignals.forEach { sampledSignal =>
      arr.push(
        InternalParentObserver.fromTry[A](sampledSignal, (_, _) => {
          // Do nothing, we just want to ensure that sampledSignal is started.
        })
      )
    }
    arr
  }

  override protected def combinedValue: Try[Out] = {
    val values = JsArray(maybeLastSamplingValue.get)
    sampledSignals.forEach { sampledSignal =>
      values.push(sampledSignal.tryNow())
    }
    CombineObservable.jsArrayCombinator(values, combinator)
  }

  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    super.syncFire(transaction)
    maybeLastSamplingValue = js.undefined // Clean up memory, as we don't need this reference anymore
  }
}
