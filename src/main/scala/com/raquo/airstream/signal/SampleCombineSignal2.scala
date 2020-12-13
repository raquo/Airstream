package com.raquo.airstream.signal

import com.raquo.airstream.features.{CombineObservable, InternalParentObserver}

import scala.util.Try

/** This signal emits the combined value when samplingSignal is updated.
  * sampledSignal's current/"latest" value is used.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param combinator Note: Must not throw. Must have no side effects. Can be executed more than once per transaction.
  */
class SampleCombineSignal2[A, B, O](
  samplingSignal: Signal[A],
  sampledSignal: Signal[B],
  combinator: (Try[A], Try[B]) => Try[O]
) extends Signal[O] with CombineObservable[O] {

  override protected[airstream] val topoRank: Int = (samplingSignal.topoRank max sampledSignal.topoRank) + 1

  override protected[this] def initialValue: Try[O] = combinator(samplingSignal.tryNow(), sampledSignal.tryNow())

  parentObservers.push(
    InternalParentObserver.fromTry[A](samplingSignal, (nextSamplingValue, transaction) => {
      // Update `maybeCombinedValue` and mark the combined observable as pending
      internalObserver.onTry(combinator(nextSamplingValue, sampledSignal.tryNow()), transaction)
    }),
    InternalParentObserver.fromTry[B](sampledSignal, (nextSampledValue, _) => {
      // Update `maybeCombinedValue`
      // - We need this if `sampledSignal` fires after the combined observable has already been marked as pending
      maybeCombinedValue = Some(combinator(samplingSignal.tryNow(), nextSampledValue))
    })
  )

}
