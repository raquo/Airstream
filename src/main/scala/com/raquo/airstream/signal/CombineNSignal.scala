package com.raquo.airstream.signal

import com.raquo.airstream.features.{ CombineObservable, InternalParentObserver }

import scala.util.Try

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
            CombineObservable.guardedSeqCombinator(trys, toOut),
            transaction
          )
        }
      )
    }: _*
  )

  protected def toOut(seq: Seq[A]): Out

  override protected[this] def initialValue: Try[Out] = {
    CombineObservable.guardedSeqCombinator(parents.map(_.tryNow()), toOut)
  }

}
