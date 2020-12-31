package com.raquo.airstream.combine.generated

import com.raquo.airstream.combine.CombineEventStreamN
import com.raquo.airstream.eventstream.EventStream

/** @param combinator Must not throw!*/
class CombineEventStream5[T1, T2, T3, T4, T5, Out](
  protected[this] val parent1: EventStream[T1],
  protected[this] val parent2: EventStream[T2],
  protected[this] val parent3: EventStream[T3],
  protected[this] val parent4: EventStream[T4],
  protected[this] val parent5: EventStream[T5],
  combinator: (T1, T2, T3, T4, T5) => Out
) extends CombineEventStreamN[Any, Out](
  parents = List(parent1, parent2, parent3, parent4, parent5),
  combinator = seq => combinator(
    seq(0).asInstanceOf[T1],
    seq(1).asInstanceOf[T2],
    seq(2).asInstanceOf[T3],
    seq(3).asInstanceOf[T4],
    seq(4).asInstanceOf[T5],
  )
)
