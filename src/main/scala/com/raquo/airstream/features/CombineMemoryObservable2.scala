package com.raquo.airstream.features

import com.raquo.airstream.core.MemoryObservable

import scala.util.Try

trait CombineMemoryObservable2[A, B, O] extends MemoryObservable[O] with CombineObservable[O] {

  /** Must not throw. Must have no side effects. It can be executed more than once per transaction. */
  protected[this] val combinator: (Try[A], Try[B]) => Try[O]

  protected[this] val parent1: MemoryObservable[A]
  protected[this] val parent2: MemoryObservable[B]

  parentObservers.push(
    InternalParentObserver.fromTry[A](parent1, (nextParent1Value, transaction) => {
      internalObserver.onTry(combinator(nextParent1Value, parent2.tryNow()), transaction)
    }),
    InternalParentObserver.fromTry[B](parent2, (nextParent2Value, transaction) => {
      internalObserver.onTry(combinator(parent1.tryNow(), nextParent2Value), transaction)
    })
  )
}
