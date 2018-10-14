package com.raquo.airstream.signal

import com.raquo.airstream.features.CombineMemoryObservable2

import scala.util.Try

/** @param combinator Note: must not throw. Must have no side effects: can be called multiple times per transaction */
class CombineSignal2[A, B, O](
  override protected[this] val parent1: Signal[A],
  override protected[this] val parent2: Signal[B],
  override protected[this] val combinator: (Try[A], Try[B]) => Try[O]
) extends Signal[O] with CombineMemoryObservable2[A, B, O] {

  override protected[airstream] val topoRank: Int = (parent1.topoRank max parent2.topoRank) + 1

  override protected[this] def initialValue: Try[O] = combinator(parent1.tryNow(), parent2.tryNow())
}
