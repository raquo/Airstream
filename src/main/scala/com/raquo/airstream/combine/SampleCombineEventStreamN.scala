package com.raquo.airstream.combine

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.InternalParentObserver
import com.raquo.airstream.signal.Signal

import scala.util.Try

/** This stream emits the combined value when samplingStreams emits.
  *
  * When the combined stream emits, it looks up the current value of sampledSignals,
  * but updates to those signals do not trigger updates to the combined stream.
  *
  * Works similar to Rx's "withLatestFrom", except without glitches (see a diamond case test for this in GlitchSpec).
  *
  * @param combinator Note: Must not throw!
  */
class SampleCombineEventStreamN[A, Out](
  samplingStream: EventStream[A],
  sampledSignals: Seq[Signal[A]],
  combinator: Seq[A] => Out
) extends EventStream[Out] with CombineObservable[Out] {

  override protected[airstream] val topoRank: Int = (samplingStream +: sampledSignals).foldLeft(0)(_ max _.topoRank) + 1

  private[this] var maybeLastSamplingValue: Option[Try[A]] = None

  override protected[this] def inputsReady: Boolean = maybeLastSamplingValue.nonEmpty

  override protected[this] def combinedValue: Try[Out] = {
    val parentValues = maybeLastSamplingValue.get +: sampledSignals.map(_.tryNow())
    CombineObservable.seqCombinator(parentValues, combinator)
  }

  parentObservers.push(
    InternalParentObserver.fromTry[A](
      samplingStream,
      (nextSamplingValue, transaction) => {
        maybeLastSamplingValue = Some(nextSamplingValue)
        onInputsReady(transaction)
      }
    )
  )

  parentObservers.push(
    sampledSignals.map { sampledSignal =>
      InternalParentObserver.fromTry[A](sampledSignal, (_, _) => {
        // Do nothing, we just want to ensure that sampledSignal is started.
      })
    }: _*
  )

  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    super.syncFire(transaction)
    maybeLastSamplingValue = None // Clean up memory, as we don't need this reference anymore
  }

}
