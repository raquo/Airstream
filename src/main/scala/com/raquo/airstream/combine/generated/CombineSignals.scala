package com.raquo.airstream.combine.generated

import com.raquo.airstream.combine.CombineSignalN
import com.raquo.airstream.signal.Signal

// These are implementations of CombineSignalN used for Signal's `combine` and `combineWith` methods

/** @param combinator Must not throw! */
class CombineSignal2[T1, T2, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  combinator: (T1, T2) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
  )
)

/** @param combinator Must not throw! */
class CombineSignal3[T1, T2, T3, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  parent3: Signal[T3],
  combinator: (T1, T2, T3) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
  )
)

/** @param combinator Must not throw! */
class CombineSignal4[T1, T2, T3, T4, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  parent3: Signal[T3],
  parent4: Signal[T4],
  combinator: (T1, T2, T3, T4) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
  )
)

/** @param combinator Must not throw! */
class CombineSignal5[T1, T2, T3, T4, T5, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  parent3: Signal[T3],
  parent4: Signal[T4],
  parent5: Signal[T5],
  combinator: (T1, T2, T3, T4, T5) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: parent5 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
    seq(4).asInstanceOf[T5],
  )
)

/** @param combinator Must not throw! */
class CombineSignal6[T1, T2, T3, T4, T5, T6, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  parent3: Signal[T3],
  parent4: Signal[T4],
  parent5: Signal[T5],
  parent6: Signal[T6],
  combinator: (T1, T2, T3, T4, T5, T6) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: parent5 :: parent6 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
    seq(4).asInstanceOf[T5],
    seq(5).asInstanceOf[T6],
  )
)

/** @param combinator Must not throw! */
class CombineSignal7[T1, T2, T3, T4, T5, T6, T7, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  parent3: Signal[T3],
  parent4: Signal[T4],
  parent5: Signal[T5],
  parent6: Signal[T6],
  parent7: Signal[T7],
  combinator: (T1, T2, T3, T4, T5, T6, T7) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: parent5 :: parent6 :: parent7 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
    seq(4).asInstanceOf[T5],
    seq(5).asInstanceOf[T6],
    seq(6).asInstanceOf[T7],
  )
)

/** @param combinator Must not throw! */
class CombineSignal8[T1, T2, T3, T4, T5, T6, T7, T8, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  parent3: Signal[T3],
  parent4: Signal[T4],
  parent5: Signal[T5],
  parent6: Signal[T6],
  parent7: Signal[T7],
  parent8: Signal[T8],
  combinator: (T1, T2, T3, T4, T5, T6, T7, T8) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: parent5 :: parent6 :: parent7 :: parent8 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
    seq(4).asInstanceOf[T5],
    seq(5).asInstanceOf[T6],
    seq(6).asInstanceOf[T7],
    seq(7).asInstanceOf[T8],
  )
)

/** @param combinator Must not throw! */
class CombineSignal9[T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
  parent1: Signal[T1],
  parent2: Signal[T2],
  parent3: Signal[T3],
  parent4: Signal[T4],
  parent5: Signal[T5],
  parent6: Signal[T6],
  parent7: Signal[T7],
  parent8: Signal[T8],
  parent9: Signal[T9],
  combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out
) extends CombineSignalN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: parent5 :: parent6 :: parent7 :: parent8 :: parent9 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
    seq(4).asInstanceOf[T5],
    seq(5).asInstanceOf[T6],
    seq(6).asInstanceOf[T7],
    seq(7).asInstanceOf[T8],
    seq(8).asInstanceOf[T9],
  )
)


