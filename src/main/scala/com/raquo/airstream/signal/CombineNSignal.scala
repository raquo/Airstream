package com.raquo.airstream.signal

import com.raquo.airstream.core.AirstreamError.CombinedError
import com.raquo.airstream.features.{ CombineObservable, InternalParentObserver }

import scala.util.{ Failure, Success, Try }

abstract class CombineNSignal[A, Out](
  protected[this] val parents: Seq[Signal[A]]
) extends Signal[Out] with CombineObservable[Out] {

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

  protected def toOut(seq: Seq[A]): Out

  private def guarded(trys: Seq[Try[A]]): Try[Out] =
    if (trys.forall(_.isSuccess)) {
      Success(toOut(trys.map(_.get)))
    } else {
      Failure(CombinedError(trys.map(_.failed.toOption)))
    }

  override protected[this] def initialValue: Try[Out] = {
    guarded(parents.map(_.tryNow()))
  }

}
