package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentSignal}
import com.raquo.airstream.core.{Observable, Protected, Signal}

import scala.util.Try

/** This signal emits the combined value when samplingSignal is updated.
  *
  * When the combined signal emits, it looks up the current value of sampledSignals,
  * but updates to those signals do not trigger updates to the combined stream.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param combinator Note: Must not throw.
  */
class SampleCombineSignalN[A, Out](
  samplingSignal: Signal[A],
  sampledSignals: Seq[Signal[A]],
  combinator: Seq[A] => Out
) extends MultiParentSignal[A, Out] with CombineObservable[Out] {

  override protected val topoRank: Int = Protected.maxParentTopoRank(samplingSignal +: sampledSignals) + 1

  override protected[this] def inputsReady: Boolean = true

  override protected[this] val parents: Seq[Observable[A]] = samplingSignal +: sampledSignals

  override protected[this] def combinedValue: Try[Out] = {
    val values = (samplingSignal +: sampledSignals).map(_.tryNow())
    CombineObservable.seqCombinator(values, combinator)
  }

  override protected def currentValueFromParent(): Try[Out] = combinedValue

  parentObservers.push(
    InternalParentObserver.fromTry[A](samplingSignal, (_, transaction) => {
      onInputsReady(transaction)
    })
  )

  parentObservers.push(
    sampledSignals.map { sampledSignal =>
      InternalParentObserver.fromTry[A](sampledSignal, (_, _) => {
        // Do nothing, we just want to ensure that sampledSignal is started.
      })
    }: _*
  )
}
