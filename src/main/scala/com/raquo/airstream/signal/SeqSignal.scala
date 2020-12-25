package com.raquo.airstream.signal

import com.raquo.airstream.core.AirstreamError.CombinedError
import com.raquo.airstream.features.{ CombineObservable, InternalParentObserver }

import scala.util.{ Failure, Success, Try }

class SeqSignal[A](
  protected[this] val parents: Seq[Signal[A]]
) extends Signal[Seq[A]]
    with CombineObservable[Seq[A]] {

  override protected[airstream] val topoRank: Int = if (parents.nonEmpty) {
    parents.map(_.topoRank).max + 1
  } else {
    1
  }

  parentObservers.push(
    parents.zipWithIndex.map { case (parent, index) =>
      InternalParentObserver.fromTry[A](
        parent,
        (nextParentValue, transaction) => {
          val trys = {
            parents.zipWithIndex.map { case (otherParent, otherIndex) =>
              if (otherIndex == index) {
                nextParentValue
              } else {
                otherParent.tryNow()
              }
            }
          }

          internalObserver.onTry(
            guarded(trys),
            transaction
          )
        }
      )
    }: _*
  )

  private def guarded(trys: Seq[Try[A]]) = {
    if (trys.forall(_.isSuccess)) {
      Success(trys.map(_.get))
    } else {
      Failure(CombinedError(trys.map(_.failed.toOption)))
    }
  }

  override protected[this] def initialValue: Try[Seq[A]] = {
    guarded(parents.map(_.tryNow()))
  }

}
