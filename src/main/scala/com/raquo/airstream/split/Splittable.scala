package com.raquo.airstream.split

import com.raquo.airstream.util.Id
import com.raquo.ew.{JsArray, JsVector, ewArray}

import scala.collection.{immutable, mutable}
import scala.scalajs.js

/** The `split` operator needs an implicit instance of Splittable[M] in order to work on observables of M[_] */
trait Splittable[M[_]] {

  def create[A](values: Seq[A]): M[A]

  /** Equivalent of list.map(project) */
  def map[A, B](inputs: M[A], project: A => B): M[B]

  /** Equivalent of list.foreach(f) */
  def foreach[A](inputs: M[A], f: A => Unit): Unit

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

  def isEmpty[A](inputs: M[A]): Boolean

  def empty[A]: M[A] = create(Nil)
}

object Splittable extends LowPrioritySplittableImplicits {

  implicit object ListSplittable extends Splittable[List] {

    override def create[A](values: Seq[A]): List[A] = values.toList

    override def map[A, B](inputs: List[A], project: A => B): List[B] = inputs.map(project)

    override def foreach[A](inputs: List[A], f: A => Unit): Unit = inputs.foreach(f)

    override def isEmpty[A](inputs: List[A]): Boolean = inputs.isEmpty
  }

  implicit object VectorSplittable extends Splittable[Vector] {

    override def create[A](values: Seq[A]): Vector[A] = values.toVector

    override def map[A, B](inputs: Vector[A], project: A => B): Vector[B] = inputs.map(project)

    override def foreach[A](inputs: Vector[A], f: A => Unit): Unit = inputs.foreach(f)

    override def isEmpty[A](inputs: Vector[A]): Boolean = inputs.isEmpty
  }

  implicit object SetSplittable extends Splittable[Set] {

    override def create[A](values: Seq[A]): Set[A] = values.toSet

    override def map[A, B](inputs: Set[A], project: A => B): Set[B] = inputs.map(project)

    override def foreach[A](inputs: Set[A], f: A => Unit): Unit = inputs.foreach(f)

    override def isEmpty[A](inputs: Set[A]): Boolean = inputs.isEmpty
  }

  implicit object BufferSplittable extends Splittable[mutable.Buffer] {

    override def create[A](values: Seq[A]): mutable.Buffer[A] = mutable.Buffer(values: _*)

    override def map[A, B](inputs: mutable.Buffer[A], project: A => B): mutable.Buffer[B] = inputs.map(project)

    override def foreach[A](inputs: mutable.Buffer[A], f: A => Unit): Unit = inputs.foreach(f)

    override def isEmpty[A](inputs: mutable.Buffer[A]): Boolean = inputs.isEmpty
  }

  implicit object JsArraySplittable extends Splittable[JsArray] {

    override def create[A](values: Seq[A]): JsArray[A] = js.Array(values: _*).ew

    override def map[A, B](inputs: JsArray[A], project: A => B): JsArray[B] = inputs.map(project)

    override def foreach[A](inputs: JsArray[A], f: A => Unit): Unit = inputs.forEach(f)

    override def isEmpty[A](inputs: JsArray[A]): Boolean = inputs.length == 0
  }

  implicit object JsVectorSplittable extends Splittable[JsVector] {

    override def create[A](values: Seq[A]): JsVector[A] = js.Array(values: _*).ew.unsafeAsJsVector // #Safe because we don't mutate the vector here

    override def map[A, B](inputs: JsVector[A], project: A => B): JsVector[B] = inputs.map(project)

    override def foreach[A](inputs: JsVector[A], f: A => Unit): Unit = inputs.forEach(f)

    override def isEmpty[A](inputs: JsVector[A]): Boolean = inputs.length == 0
  }

  implicit object ScalaJsArraySplittable extends Splittable[js.Array] {

    override def create[A](values: Seq[A]): js.Array[A] = js.Array(values: _*)

    override def map[A, B](inputs: js.Array[A], project: A => B): js.Array[B] = inputs.map(project)

    override def foreach[A](inputs: js.Array[A], f: A => Unit): Unit = inputs.foreach(f)

    override def isEmpty[A](inputs: js.Array[A]): Boolean = inputs.isEmpty
  }

  /** Used for splitOption. Not `implicit`, to avoid accidental use with splitSeq.
    * It's unsafe because `create` is impossible to implement fully.
    * We can still use it carefully in cases when we know that this method
    * will not be called, for example, splitting observables of Option[A] with splitOption.
    */
  object UnsafeOptionSplittable extends Splittable[Option] {

    override def create[A](values: Seq[A]): Option[A] = {
      throw new Exception("Accessing UnsafeOptionSplittable.create is not allowed!")
    }

    override def map[A, B](inputs: Option[A], project: A => B): Option[B] = inputs.map(project)

    override def foreach[A](inputs: Option[A], f: A => Unit): Unit = inputs.foreach(f)

    override def isEmpty[A](inputs: Option[A]): Boolean = inputs.isEmpty
  }

  /** Used for splitOne. Not `implicit`, to avoid accidental use with splitSeq.
    * It's unsafe because `create` and `empty` Are impossible to implement.
    * We can still use it carefully in cases when we know that these methods
    * will not be called, for example, splitting signals of A with splitOne.
    * But not in streams.
    */
  object UnsafeIdSplittable extends Splittable[Id] {

    override def create[A](values: Seq[A]): Id[A] = {
      throw new Exception("Accessing UnsafeIdSplittable.create is not allowed!")
    }

    override def map[A, B](inputs: Id[A], project: A => B): Id[B] = project(inputs)

    override def foreach[A](inputs: Id[A], f: A => Unit): Unit = f(inputs)

    override def isEmpty[A](inputs: Id[A]): Boolean = false

    override def empty[A]: Id[A] = {
      throw new Exception("Accessing UnsafeIdSplittable.empty is not allowed!")
    }
  }
}

/** If changing the order, check it first on all Scala versions (and be disappointed).
  * Some of the errors caused by implicit ambiguity are not very deterministic in Scala 3. Be conservative.
  */
trait LowPrioritySplittableImplicits extends LowestPrioritySplittableImplicits {

  implicit object ImmutableSeqSplittable extends Splittable[immutable.Seq] {

    override def create[A](values: Seq[A]): Seq[A] = values

    override def map[A, B](inputs: immutable.Seq[A], project: A => B): immutable.Seq[B] = {
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.map(project)
    }

    override def foreach[A](inputs: Seq[A], f: A => Unit): Unit = {
      // #TODO[Perf] Do we actually need to force .toList here? I think LazyList.foreach is strict already.
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.foreach(f)
    }

    override def isEmpty[A](inputs: Seq[A]): Boolean = inputs.isEmpty

    override def empty[A]: immutable.Seq[A] = Nil
  }
}

trait LowestPrioritySplittableImplicits {

  implicit object SeqSplittable extends Splittable[collection.Seq] {

    override def create[A](values: Seq[A]): collection.Seq[A] = values

    override def map[A, B](inputs: collection.Seq[A], project: A => B): collection.Seq[B] = {
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.map(project)
    }

    override def foreach[A](inputs: collection.Seq[A], f: A => Unit): Unit = {
      // #TODO[Perf] Do we actually need to force .toList here? I think LazyList.foreach is strict already.
      val strictInputs = inputs match {
        case lazyList: LazyList[A @unchecked] => lazyList.toList
        case _ => inputs
      }
      strictInputs.foreach(f)
    }

    override def isEmpty[A](inputs: collection.Seq[A]): Boolean = inputs.isEmpty

    override def empty[A]: collection.Seq[A] = Nil
  }
}
