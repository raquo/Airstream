package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.features.{CombineObservable, InternalParentObserver}
import com.raquo.airstream.signal.Signal

import scala.util.Try

/** This stream emits the combined value when samplingStream is updated.
  * sampledSignal's current/"latest" value is used.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param combinator Note: Must not throw. Must have no side effects. Can be executed more than once per transaction.
  */
class SampleCombineEventStream2[A, B, O](
  samplingStream: EventStream[A],
  sampledSignal: Signal[B],
  combinator: (Try[A], Try[B]) => Try[O]
) extends EventStream[O] with CombineObservable[O] {

  override protected[airstream] val topoRank: Int = (samplingStream.topoRank max sampledSignal.topoRank) + 1

  private[this] var maybeSamplingValue: Option[Try[A]] = None

  parentObservers.push(
    InternalParentObserver.fromTry[A](samplingStream, (nextSamplingValue, transaction) => {
      maybeSamplingValue = Some(nextSamplingValue)
      internalObserver.onTry(combinator(nextSamplingValue, sampledSignal.tryNow()), transaction)
    }),
    InternalParentObserver.fromTry[B](sampledSignal, (nextSampledValue, _) => {
      // Update combined value, but only if sampling stream already emitted a value.
      // So we only update the value if we know that this observable will syncFire.
      maybeSamplingValue.foreach { lastSamplingValue =>
        maybeCombinedValue = Some(combinator(lastSamplingValue, nextSampledValue))
      }
    })
  )

  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    maybeSamplingValue = None // This is set to none only if syncFire is imminent, so this is enough to clear memory
    super.syncFire(transaction)
  }

}
