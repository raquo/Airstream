package com.raquo.airstream.combine.generated

import com.raquo.airstream.combine.CombineEventStreamN
import com.raquo.airstream.eventstream.EventStream

/** @param combinator Must not throw!*/
class CombineEventStream2[T1, T2, Out](
  protected[this] val parent1: EventStream[T1],
  protected[this] val parent2: EventStream[T2],
  combinator: (T1, T2) => Out
) extends CombineEventStreamN[Any, Out](
  parents = List(parent1, parent2),
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
  )
)
