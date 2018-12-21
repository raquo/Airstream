package com.raquo.airstream.signal

import com.raquo.airstream.features.{CombineObservable, InternalParentObserver}

import scala.util.Try

/** @param combinator Note: must not throw. Must have no side effects: can be called multiple times per transaction */
class CombineSignal2[A, B, O](
  protected[this] val parent1: Signal[A],
  protected[this] val parent2: Signal[B],
  protected[this] val combinator: (Try[A], Try[B]) => Try[O]
) extends Signal[O] with CombineObservable[O] {

  override protected[airstream] val topoRank: Int = (parent1.topoRank max parent2.topoRank) + 1

   parentObservers.push(
    InternalParentObserver.fromTry[A](parent1, (nextParent1Value, transaction) => {
      internalObserver.onTry(combinator(nextParent1Value, parent2.tryNow()), transaction)
    }),
    InternalParentObserver.fromTry[B](parent2, (nextParent2Value, transaction) => {
      internalObserver.onTry(combinator(parent1.tryNow(), nextParent2Value), transaction)
    })
  )

  override protected[this] def initialValue: Try[O] = combinator(parent1.tryNow(), parent2.tryNow())
}
