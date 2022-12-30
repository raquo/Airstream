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
  * @param sampledSignals - Never update this array - this signal owns it.
  *
  * @param combinator Note: Must not throw! Must be pure.
  */
class SampleCombineSignalN[A, Out](
  samplingSignal: Signal[A],
  sampledSignals: JsArray[Signal[A]],
  combinator: JsArray[A] => Out
) extends MultiParentSignal[A, Out] with CombineObservable[Out] {

  override protected val topoRank: Int = Protected.maxTopoRank(samplingSignal, sampledSignals) + 1

  override protected[this] def inputsReady: Boolean = true

  override protected[this] val parents: JsArray[Signal[A]] = combineWithArray(
    samplingSignal,
    sampledSignals
  )

  override protected[this] def combinedValue: Try[Out] = {
    val values = combineWithArray(
      samplingSignal.tryNow(),
      sampledSignals.map(_.tryNow())
    )
    CombineObservable.jsArrayCombinator(values, combinator)
  }

  override protected def currentValueFromParent(): Try[Out] = combinedValue

  parentObservers.push(
    InternalParentObserver.fromTry[A](samplingSignal, (_, transaction) => {
      onInputsReady(transaction)
    })
  )

  sampledSignals.forEach { sampledSignal =>
    parentObservers.push(
      InternalParentObserver.fromTry[A](sampledSignal, (_, _) => {
        // Do nothing, we just want to ensure that sampledSignal is started.
      })
    )
  }

  private[this] def combineWithArray[V](sampling: V, sampled: JsArray[V]): JsArray[V] = {
    val values = sampled.concat() // There's also JsArray.from, but it does not work in IE11
    values.unshift(sampling)
    values
  }
}
