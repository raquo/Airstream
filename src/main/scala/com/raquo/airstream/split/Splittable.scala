package com.raquo.airstream.split

import com.raquo.airstream.util.Id

import scala.collection.immutable
import scala.scalajs.js

trait Splittable[M[_]] {

  def map[A, B](inputs: M[A], project: A => B): M[B]
}

object Splittable {

  // @TODO[Elegance] This is laaame, but I don't want to get into CanBuildFrom as it's being phased out. Improve later.

  implicit object ListSplittable extends Splittable[List] {
    override def map[A, B](inputs: List[A], project: A => B): List[B] = inputs.map(project)
  }

  implicit object VectorSplittable extends Splittable[Vector] {
    override def map[A, B](inputs: Vector[A], project: A => B): Vector[B] = inputs.map(project)
  }

  implicit object SetSplittable extends Splittable[Set] {
    override def map[A, B](inputs: Set[A], project: A => B): Set[B] = inputs.map(project)
  }

  implicit object JsArraySplittable extends Splittable[js.Array] {
    override def map[A, B](inputs: js.Array[A], project: A => B): js.Array[B] = inputs.map(project)
  }

  implicit object ImmutableSeqSplittable extends Splittable[immutable.Seq] {
    override def map[A, B](inputs: immutable.Seq[A], project: A => B): immutable.Seq[B] = inputs.map(project)
  }

  implicit object SeqSplittable extends Splittable[collection.Seq] {
    override def map[A, B](inputs: collection.Seq[A], project: A => B): collection.Seq[B] = inputs.map(project)
  }

  implicit object OptionSplittable extends Splittable[Option] {
    override def map[A, B](inputs: Option[A], project: A => B): Option[B] = inputs.map(project)
  }

  /** Used by `splitOne` and `splitOneIntoSignals` methods */
  object IdSplittable extends Splittable[Id] {
    override def map[A, B](input: A, project: A => B): B = project(input)
  }
}
