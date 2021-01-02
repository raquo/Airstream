package com.raquo.airstream.combine.generated

import com.raquo.airstream.combine.CombineEventStreamN
import com.raquo.airstream.eventstream.EventStream

// These are implementations of CombineEventStreamN used for EventStream's `combine` and `combineWith` methods

/** @param combinator Must not throw! */
class CombineEventStream2[T1, T2, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  combinator: (T1, T2) => Out
) extends CombineEventStreamN[Any, Out](
  parents = parent1 :: parent2 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
  )
)

/** @param combinator Must not throw! */
class CombineEventStream3[T1, T2, T3, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  combinator: (T1, T2, T3) => Out
) extends CombineEventStreamN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
  )
)

/** @param combinator Must not throw! */
class CombineEventStream4[T1, T2, T3, T4, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  parent4: EventStream[T4],
  combinator: (T1, T2, T3, T4) => Out
) extends CombineEventStreamN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: Nil,
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
  )
)

/** @param combinator Must not throw! */
class CombineEventStream5[T1, T2, T3, T4, T5, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  parent4: EventStream[T4],
  parent5: EventStream[T5],
  combinator: (T1, T2, T3, T4, T5) => Out
) extends CombineEventStreamN[Any, Out](
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
class CombineEventStream6[T1, T2, T3, T4, T5, T6, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  parent4: EventStream[T4],
  parent5: EventStream[T5],
  parent6: EventStream[T6],
  combinator: (T1, T2, T3, T4, T5, T6) => Out
) extends CombineEventStreamN[Any, Out](
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
class CombineEventStream7[T1, T2, T3, T4, T5, T6, T7, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  parent4: EventStream[T4],
  parent5: EventStream[T5],
  parent6: EventStream[T6],
  parent7: EventStream[T7],
  combinator: (T1, T2, T3, T4, T5, T6, T7) => Out
) extends CombineEventStreamN[Any, Out](
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
class CombineEventStream8[T1, T2, T3, T4, T5, T6, T7, T8, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  parent4: EventStream[T4],
  parent5: EventStream[T5],
  parent6: EventStream[T6],
  parent7: EventStream[T7],
  parent8: EventStream[T8],
  combinator: (T1, T2, T3, T4, T5, T6, T7, T8) => Out
) extends CombineEventStreamN[Any, Out](
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
class CombineEventStream9[T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  parent4: EventStream[T4],
  parent5: EventStream[T5],
  parent6: EventStream[T6],
  parent7: EventStream[T7],
  parent8: EventStream[T8],
  parent9: EventStream[T9],
  combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out
) extends CombineEventStreamN[Any, Out](
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

/** @param combinator Must not throw! */
class CombineEventStream10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Out](
  parent1: EventStream[T1],
  parent2: EventStream[T2],
  parent3: EventStream[T3],
  parent4: EventStream[T4],
  parent5: EventStream[T5],
  parent6: EventStream[T6],
  parent7: EventStream[T7],
  parent8: EventStream[T8],
  parent9: EventStream[T9],
  parent10: EventStream[T10],
  combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => Out
) extends CombineEventStreamN[Any, Out](
  parents = parent1 :: parent2 :: parent3 :: parent4 :: parent5 :: parent6 :: parent7 :: parent8 :: parent9 :: parent10 :: Nil,
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
    seq(9).asInstanceOf[T10],
  )
)

