package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentSignal}
import com.raquo.airstream.core.{Protected, Signal}
import com.raquo.ew.JsArray

import scala.util.Try

/** This signal emits the combined value when samplingSignal is updated.
  *
  * When the combined signal emits, it looks up the current value of sampledSignals,
  * but updates to those signals do not trigger updates to the combined stream.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param sampledSignals Never update this array - this signal owns it.
  * @param combinator     Note: Must not throw! Must be pure.
  */
class SampleCombineSignalN[A, Out](
  samplingSignal: Signal[A],
  sampledSignals: JsArray[Signal[A]],
  combinator: JsArray[A] => Out
) extends MultiParentSignal[A, Out] with CombineObservable[Out] {

  override protected val topoRank: Int = Protected.maxTopoRank(samplingSignal, sampledSignals) + 1

  override protected def inputsReady: Boolean = true

  override protected val parents: JsArray[Signal[A]] = {
    val arr = JsArray(samplingSignal)
    sampledSignals.forEach { sampledSignal =>
      arr.push(sampledSignal)
    }
    arr
  }

  override protected val parentObservers: JsArray[InternalParentObserver[?]] = {
    val arr = JsArray[InternalParentObserver[?]](
      InternalParentObserver.fromTry[A](samplingSignal, (_, trx) => {
        onInputsReady(trx)
      })
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
    val values = JsArray(samplingSignal.tryNow())
    sampledSignals.forEach { sampledSignal =>
      values.push(sampledSignal.tryNow())
    }
    CombineObservable.jsArrayCombinator(values, combinator)
  }

  override protected def currentValueFromParent(): Try[Out] = combinedValue
}
