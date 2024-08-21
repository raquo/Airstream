package com.raquo.airstream.split

import com.raquo.ew.{JsArray, JsVector}

import scala.collection.{immutable, mutable}
import scala.scalajs.js

/** The `split` operator needs an implicit instance of Splittable[M] in order to work on observables of M[_] */
trait Splittable[M[_]] {

  /** Equivalent of list.map(project) */
  def map[A, B](inputs: M[A], project: A => B): M[B]

  /** Equivalent of list.foreach(f) */
  def foreach[A](inputs: M[A], f: A => Unit): Unit = {
    // #TODO[API] This default implementation is inefficient,
    //  we're only adding it to satisfy binary compatibility in 17.1.0.
    //  #nc Remove this implementation later.
    map(inputs, f)
  }

  /** Find the FIRST item matching predicate, and return a collection with it updated */
  def findUpdate[A](inputs: M[A], predicate: A => Boolean, newItem: A): M[A] = {
    // #TODO should we try to provide more efficient implementations for:
    //  Vector, ArraySeq, ArrayBuffer, JsArray, js.Array, JsVector?
    //  - but what if we have e.g. ArrayBuffer typed as Buffer? Don't want runtime type checks...
    //  - is the impl below actually less efficient than using indexWhere?
    var found = false // keys must be unique, so we can break early
    map[A, A](inputs, item => {
      if (!found && predicate(item)) {
        found = true
        newItem
      } else {
        item
      }
    })
  }

  def findUpdateByIndex[A](inputs: M[A], index: Int, newItem: A): M[A] = {
    var ix = -1
    map(inputs, (input: A) => {
      ix += 1
      if (ix == index) {
        newItem
      } else {
        input
      }
    })
  }

  def zipWithIndex[A](inputs: M[A]): M[(A, Int)] = {
    var ix = -1
    map(inputs, (input: A) => {
      ix += 1
      (input, ix)
    })
  }

  def empty[A]: M[A]
}

object Splittable extends LowPrioritySplittableImplicits {

  implicit object ListSplittable extends Splittable[List] {

    override def map[A, B](inputs: List[A], project: A => B): List[B] = inputs.map(project)

    override def foreach[A](inputs: List[A], f: A => Unit): Unit = inputs.foreach(f)

    override def empty[A]: List[A] = Nil
  }

  implicit object VectorSplittable extends Splittable[Vector] {

    override def map[A, B](inputs: Vector[A], project: A => B): Vector[B] = inputs.map(project)

    override def foreach[A](inputs: Vector[A], f: A => Unit): Unit = inputs.foreach(f)

    override def empty[A]: Vector[A] = Vector.empty
  }

  implicit object SetSplittable extends Splittable[Set] {

    override def map[A, B](inputs: Set[A], project: A => B): Set[B] = inputs.map(project)

    override def foreach[A](inputs: Set[A], f: A => Unit): Unit = inputs.foreach(f)

    override def empty[A]: Set[A] = Set.empty
  }

  implicit object OptionSplittable extends Splittable[Option] {

    override def map[A, B](inputs: Option[A], project: A => B): Option[B] = inputs.map(project)

    override def foreach[A](inputs: Option[A], f: A => Unit): Unit = inputs.foreach(f)

    override def empty[A]: Option[A] = None
  }

  implicit object BufferSplittable extends Splittable[mutable.Buffer] {

    override def map[A, B](inputs: mutable.Buffer[A], project: A => B): mutable.Buffer[B] = inputs.map(project)

    override def foreach[A](inputs: mutable.Buffer[A], f: A => Unit): Unit = inputs.foreach(f)

    override def empty[A]: mutable.Buffer[A] = mutable.Buffer()
  }

  implicit object JsArraySplittable extends Splittable[JsArray] {

    override def map[A, B](inputs: JsArray[A], project: A => B): JsArray[B] = inputs.map(project)

    override def foreach[A](inputs: JsArray[A], f: A => Unit): Unit = inputs.forEach(f)

    override def empty[A]: JsArray[A] = JsArray()
  }

  implicit object JsVectorSplittable extends Splittable[JsVector] {

    override def map[A, B](inputs: JsVector[A], project: A => B): JsVector[B] = inputs.map(project)

    override def foreach[A](inputs: JsVector[A], f: A => Unit): Unit = inputs.forEach(f)

    override def empty[A]: JsVector[A] = JsVector()
  }

  implicit object ScalaJsArraySplittable extends Splittable[js.Array] {

    override def map[A, B](inputs: js.Array[A], project: A => B): js.Array[B] = inputs.map(project)

    override def foreach[A](inputs: js.Array[A], f: A => Unit): Unit = inputs.foreach(f)

    override def empty[A]: js.Array[A] = js.Array()
  }
}

/** If changing the order, check it first on all Scala versions (and be disappointed).
  * Some of the errors caused by implicit ambiguity are not very deterministic in Scala 3. Be conservative.
  */
trait LowPrioritySplittableImplicits extends LowestPrioritySplittableImplicits {

  implicit object ImmutableSeqSplittable extends Splittable[immutable.Seq] {

    override def map[A, B](inputs: immutable.Seq[A], project: A => B): immutable.Seq[B] = {
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.map(project)
    }

    override def foreach[A](inputs: Seq[A], f: A => Unit): Unit = {
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.foreach(f)
    }

    override def empty[A]: immutable.Seq[A] = Nil
  }
}

trait LowestPrioritySplittableImplicits {

  implicit object SeqSplittable extends Splittable[collection.Seq] {

    override def map[A, B](inputs: collection.Seq[A], project: A => B): collection.Seq[B] = {
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.map(project)
    }

    override def foreach[A](inputs: collection.Seq[A], f: A => Unit): Unit = {
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.foreach(f)
    }

    override def empty[A]: collection.Seq[A] = Nil
  }
}
