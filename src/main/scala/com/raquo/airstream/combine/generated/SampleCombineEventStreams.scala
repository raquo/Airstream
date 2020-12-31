package com.raquo.airstream.combine.generated

import com.raquo.airstream.combine.SampleCombineEventStreamN
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal

/** @param combinator Must not throw! */
class SampleCombineEventStream2[T0, T1, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  combinator: (T0, T1) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream3[T0, T1, T2, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  combinator: (T0, T1, T2) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream4[T0, T1, T2, T3, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  combinator: (T0, T1, T2, T3) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream5[T0, T1, T2, T3, T4, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  combinator: (T0, T1, T2, T3, T4) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: sampledSignal4 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
    seq(4).asInstanceOf[T4],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream6[T0, T1, T2, T3, T4, T5, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  combinator: (T0, T1, T2, T3, T4, T5) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: sampledSignal4 :: sampledSignal5 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
    seq(4).asInstanceOf[T4],
    seq(5).asInstanceOf[T5],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream7[T0, T1, T2, T3, T4, T5, T6, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  sampledSignal6: Signal[T6],
  combinator: (T0, T1, T2, T3, T4, T5, T6) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: sampledSignal4 :: sampledSignal5 :: sampledSignal6 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
    seq(4).asInstanceOf[T4],
    seq(5).asInstanceOf[T5],
    seq(6).asInstanceOf[T6],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream8[T0, T1, T2, T3, T4, T5, T6, T7, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  sampledSignal6: Signal[T6],
  sampledSignal7: Signal[T7],
  combinator: (T0, T1, T2, T3, T4, T5, T6, T7) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: sampledSignal4 :: sampledSignal5 :: sampledSignal6 :: sampledSignal7 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
    seq(4).asInstanceOf[T4],
    seq(5).asInstanceOf[T5],
    seq(6).asInstanceOf[T6],
    seq(7).asInstanceOf[T7],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream9[T0, T1, T2, T3, T4, T5, T6, T7, T8, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  sampledSignal6: Signal[T6],
  sampledSignal7: Signal[T7],
  sampledSignal8: Signal[T8],
  combinator: (T0, T1, T2, T3, T4, T5, T6, T7, T8) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: sampledSignal4 :: sampledSignal5 :: sampledSignal6 :: sampledSignal7 :: sampledSignal8 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
    seq(4).asInstanceOf[T4],
    seq(5).asInstanceOf[T5],
    seq(6).asInstanceOf[T6],
    seq(7).asInstanceOf[T7],
    seq(8).asInstanceOf[T8],
  )
)

/** @param combinator Must not throw! */
class SampleCombineEventStream10[T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
  samplingStream: EventStream[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  sampledSignal6: Signal[T6],
  sampledSignal7: Signal[T7],
  sampledSignal8: Signal[T8],
  sampledSignal9: Signal[T9],
  combinator: (T0, T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out
) extends SampleCombineEventStreamN[Any, Out](
  samplingStream = samplingStream,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: sampledSignal4 :: sampledSignal5 :: sampledSignal6 :: sampledSignal7 :: sampledSignal8 :: sampledSignal9 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
    seq(4).asInstanceOf[T4],
    seq(5).asInstanceOf[T5],
    seq(6).asInstanceOf[T6],
    seq(7).asInstanceOf[T7],
    seq(8).asInstanceOf[T8],
    seq(9).asInstanceOf[T9],
  )
)


