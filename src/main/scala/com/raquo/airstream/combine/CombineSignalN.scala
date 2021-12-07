package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentSignal}
import com.raquo.airstream.core.{Protected, Signal}

import scala.util.Try

/** @param combinator Must not throw! */
class CombineSignalN[A, Out](
  override protected[this] val parents: Seq[Signal[A]],
  protected[this] val combinator: Seq[A] => Out
) extends MultiParentSignal[A, Out] with CombineObservable[Out] {

  // @TODO[API] Maybe this should throw if parents.isEmpty

  override protected val topoRank: Int = Protected.maxTopoRank(parents) + 1

  override protected[this] def inputsReady: Boolean = true

  override protected[this] def combinedValue: Try[Out] = {
    CombineObservable.seqCombinator(parents.map(_.tryNow()), combinator)
  }

  override protected def currentValueFromParent(): Try[Out] = combinedValue

  parentObservers.push(
    parents.map { parent =>
      InternalParentObserver.fromTry[A](parent, (_, transaction) => {
        onInputsReady(transaction)
      })
    }: _*
  )

}
