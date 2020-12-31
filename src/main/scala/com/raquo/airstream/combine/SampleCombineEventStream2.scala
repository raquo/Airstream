package com.raquo.airstream.combine

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.InternalParentObserver
import com.raquo.airstream.signal.Signal

import scala.util.Try

/** This stream emits the combined value when samplingStream is updated.
  * sampledSignal's current/"latest" value is used.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param combinator Note: Must not throw.
  */
class SampleCombineEventStream2[A, B, O](
  samplingStream: EventStream[A],
  sampledSignal: Signal[B],
  combinator: (Try[A], Try[B]) => Try[O]
) extends EventStream[O] with CombineObservable[O] {

  override protected[airstream] val topoRank: Int = (samplingStream.topoRank max sampledSignal.topoRank) + 1

  private[this] var maybeSamplingValue: Option[Try[A]] = None

  override protected[this] def inputsReady: Boolean = maybeSamplingValue.nonEmpty

  override protected[this] def combinedValue: Try[O] = {
    combinator(maybeSamplingValue.get, sampledSignal.tryNow())
  }

  parentObservers.push(
    InternalParentObserver.fromTry[A](samplingStream, (nextSamplingValue, transaction) => {
      maybeSamplingValue = Some(nextSamplingValue)
      onInputsReady(transaction)
    }),
    InternalParentObserver.fromTry[B](sampledSignal, (_, _) => {
      // Do nothing, we just want to ensure that sampledSignal is started.
    })
  )

  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    super.syncFire(transaction)
    maybeSamplingValue = None // Clear memory, as we won't need this value again
  }

}
