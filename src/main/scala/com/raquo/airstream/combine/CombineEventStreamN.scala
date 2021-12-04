package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentEventStream}
import com.raquo.airstream.core.{EventStream, Protected}

import scala.util.Try

/** @param combinator Must not throw! */
class CombineEventStreamN[A, Out](
  override protected[this] val parents: Seq[EventStream[A]],
  combinator: Seq[A] => Out
) extends MultiParentEventStream[A, Out] with CombineObservable[Out] {

  // @TODO[API] Maybe this should throw if parents.isEmpty

  override protected val topoRank: Int = Protected.maxParentTopoRank(parents) + 1

  private[this] val maybeLastParentValues: Array[Option[Try[A]]] = Array.fill(parents.size)(None)

  override protected[this] def inputsReady: Boolean = {
    maybeLastParentValues.forall(_.nonEmpty)
  }

  override protected[this] def combinedValue: Try[Out] = {
    // @TODO[Scala3] When we don't need Scala 2.12, use ArraySeq.unsafeWrapArray(maybeLastParentValues) for perf?
    CombineObservable.seqCombinator(maybeLastParentValues.toIndexedSeq.map(_.get), combinator)
  }

  parentObservers.push(
    parents.zipWithIndex.map { case (parent, index) =>
      InternalParentObserver.fromTry[A](
        parent,
        (nextParentValue, transaction) => {
          maybeLastParentValues.update(index, Some(nextParentValue))
          if (inputsReady) {
            onInputsReady(transaction)
          }
        }
      )
    }: _*
  )

}
