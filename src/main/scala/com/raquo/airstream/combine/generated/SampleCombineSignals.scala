package com.raquo.airstream.combine.generated

import com.raquo.airstream.combine.SampleCombineSignalN
import com.raquo.airstream.signal.Signal

// These are implementations of SampleCombineSignalN used for Signal's `withCurrentValueOf` and `sample` methods

/** @param combinator Must not throw! */
class SampleCombineSignal2[T0, T1, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  combinator: (T0, T1) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
  sampledSignals = sampledSignal1 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
  )
)

/** @param combinator Must not throw! */
class SampleCombineSignal3[T0, T1, T2, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  combinator: (T0, T1, T2) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
  )
)

/** @param combinator Must not throw! */
class SampleCombineSignal4[T0, T1, T2, T3, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  combinator: (T0, T1, T2, T3) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
  sampledSignals = sampledSignal1 :: sampledSignal2 :: sampledSignal3 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T0],
    seq(1).asInstanceOf[T1],
    seq(2).asInstanceOf[T2],
    seq(3).asInstanceOf[T3],
  )
)

/** @param combinator Must not throw! */
class SampleCombineSignal5[T0, T1, T2, T3, T4, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  combinator: (T0, T1, T2, T3, T4) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
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
class SampleCombineSignal6[T0, T1, T2, T3, T4, T5, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  combinator: (T0, T1, T2, T3, T4, T5) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
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
class SampleCombineSignal7[T0, T1, T2, T3, T4, T5, T6, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  sampledSignal6: Signal[T6],
  combinator: (T0, T1, T2, T3, T4, T5, T6) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
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
class SampleCombineSignal8[T0, T1, T2, T3, T4, T5, T6, T7, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  sampledSignal6: Signal[T6],
  sampledSignal7: Signal[T7],
  combinator: (T0, T1, T2, T3, T4, T5, T6, T7) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
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
class SampleCombineSignal9[T0, T1, T2, T3, T4, T5, T6, T7, T8, Out](
  samplingSignal: Signal[T0],
  sampledSignal1: Signal[T1],
  sampledSignal2: Signal[T2],
  sampledSignal3: Signal[T3],
  sampledSignal4: Signal[T4],
  sampledSignal5: Signal[T5],
  sampledSignal6: Signal[T6],
  sampledSignal7: Signal[T7],
  sampledSignal8: Signal[T8],
  combinator: (T0, T1, T2, T3, T4, T5, T6, T7, T8) => Out
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
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
class SampleCombineSignal10[T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
  samplingSignal: Signal[T0],
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
) extends SampleCombineSignalN[Any, Out](
  samplingSignal = samplingSignal,
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


