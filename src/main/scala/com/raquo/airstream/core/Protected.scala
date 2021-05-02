package com.raquo.airstream.core

import scala.annotation.{implicitNotFound, unused}
import scala.util.Try

@implicitNotFound("Implicit instance of Airstream's `Protected` class not found. You're trying to access a method which is designed to only be accessed from inside a BaseObservable subtype.")
class Protected private ()

object Protected {

  private[airstream] implicit val protectedAccessEvidence: Protected = new Protected()

  @inline def topoRank[O[+_] <: Observable[_]](observable: BaseObservable[O, _]): Int = {
    BaseObservable.topoRank(observable)
  }

  def maxParentTopoRank[O[+_] <: Observable[_]](parents: Iterable[BaseObservable[O, _]]): Int = {
    parents.foldLeft(0)((maxRank, parent) => Protected.topoRank(parent) max maxRank)
  }

  def tryNow[A](signal: Signal[A])(implicit @unused ev: Protected): Try[A] = signal.tryNow()
}
