package com.raquo.airstream.combine

import com.raquo.airstream.common.InternalParentObserver
import com.raquo.airstream.core.{Protected, Signal, WritableSignal}

import scala.util.Try

/** @param combinator Must not throw! */
class CombineSignalN[A, Out](
  protected[this] val parents: Seq[Signal[A]],
  protected[this] val combinator: Seq[A] => Out
) extends WritableSignal[Out] with CombineObservable[Out] {

  // @TODO[API] Maybe this should throw if parents.isEmpty

  override protected val topoRank: Int = Protected.maxParentTopoRank(parents) + 1

  override protected[this] def initialValue: Try[Out] = combinedValue

  override protected[this] def inputsReady: Boolean = true

  override protected[this] def combinedValue: Try[Out] = {
    CombineObservable.seqCombinator(parents.map(_.tryNow()), combinator)
  }

  parentObservers.push(
    parents.map { parent =>
      InternalParentObserver.fromTry[A](parent, (_, transaction) => {
        onInputsReady(transaction)
      })
    }: _*
  )

}
