package com.raquo.airstream.combine

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.InternalParentObserver

import scala.collection.immutable.ArraySeq
import scala.util.Try

/** @param combinator Must not throw! */
class CombineEventStreamN[A, Out](
  protected[this] val parents: Seq[EventStream[A]],
  protected[this] val combinator: Seq[A] => Out
) extends EventStream[Out] with CombineObservable[Out] {

  // @TODO[API] Maybe this should throw if parents.isEmpty

  override protected[airstream] val topoRank: Int = parents.foldLeft(0)(_ max _.topoRank) + 1

  private[this] val maybeLastParentValues: Array[Option[Try[A]]] = Array.fill(parents.size)(None)

  override protected[this] def inputsReady: Boolean = {
    maybeLastParentValues.forall(_.nonEmpty)
  }

  override protected[this] def combinedValue: Try[Out] = {
    CombineObservable.seqCombinator(ArraySeq.unsafeWrapArray(maybeLastParentValues).map(_.get), combinator)
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

  override protected[this] def onStop(): Unit = {
    maybeLastParentValues.indices.foreach(maybeLastParentValues.update(_, None))
    super.onStop()
  }

}
