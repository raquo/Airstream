package com.raquo.airstream.combine

import com.raquo.airstream.features.InternalParentObserver
import com.raquo.airstream.signal.Signal

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
) extends Signal[Out] with CombineObservable[Out] {

  override protected[airstream] val topoRank: Int = (samplingSignal +: sampledSignals).foldLeft(0)(_ max _.topoRank) + 1

  override protected[this] def initialValue: Try[Out] = combinedValue

  override protected[this] def inputsReady: Boolean = true

  override protected[this] def combinedValue: Try[Out] = {
    val values = (samplingSignal +: sampledSignals).map(_.tryNow())
    CombineObservable.seqCombinator(values, combinator)
  }

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
