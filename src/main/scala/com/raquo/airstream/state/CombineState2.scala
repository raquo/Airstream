package com.raquo.airstream.state

import com.raquo.airstream.features.CombineMemoryObservable2
import com.raquo.airstream.ownership.Owner

import scala.util.Try

/** @param combinator Note: must not throw. Must have no side effects: can be called multiple times per transaction */
class CombineState2[A, B, O](
  override protected[this] val parent1: State[A],
  override protected[this] val parent2: State[B],
  override protected[this] val combinator: (Try[A], Try[B]) => Try[O],
  override protected[state] val owner: Owner
) extends State[O] with CombineMemoryObservable2[A, B, O] {

  override protected[airstream] val topoRank: Int = (parent1.topoRank max parent2.topoRank) + 1

  init()

  onStart()

  override protected[this] def initialValue: Try[O] = combinator(parent1.tryNow(), parent2.tryNow())

}
