package com.raquo.airstream.combine

import com.raquo.airstream.features.InternalParentObserver
import com.raquo.airstream.signal.Signal

import scala.util.Try

/** This signal emits the combined value when samplingSignal is updated.
  * sampledSignal's current/"latest" value is used.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param combinator Note: Must not throw.
  */
class SampleCombineSignal2[A, B, O](
  samplingSignal: Signal[A],
  sampledSignal: Signal[B],
  combinator: (Try[A], Try[B]) => Try[O]
) extends Signal[O] with CombineObservable[O] {

  override protected[airstream] val topoRank: Int = (samplingSignal.topoRank max sampledSignal.topoRank) + 1

  override protected[this] def initialValue: Try[O] = combinator(samplingSignal.tryNow(), sampledSignal.tryNow())

  override protected[this] def inputsReady: Boolean = true

  override protected[this] def combinedValue: Try[O] = {
    combinator(samplingSignal.tryNow(), sampledSignal.tryNow())
  }

  parentObservers.push(
    InternalParentObserver.fromTry[A](samplingSignal, (_, transaction) => {
      onInputsReady(transaction)
    }),
    InternalParentObserver.fromTry[B](sampledSignal, (_, _) => {
      // Do nothing, we just want to ensure that sampledSignal is started.
    })
  )

}
