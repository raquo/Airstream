package com.raquo.airstream.split

import com.raquo.ew.JsArray

import scala.collection.mutable
import scala.scalajs.js

trait MutableSplittable[M[_]] {

  val splittable: Splittable[M]

  def size[A](items: M[A]): Int

  /** Equivalent of array(index) */
  def getByIndex[A](items: M[A], index: Int): A

  /** Equivalent of array.update(index, newValue) */
  def updateAtIndex[A](items: M[A], index: Int, newItem: A): Unit

  /** Equivalent of array.find(predicate).foreach(array.update(foundIndex, newItem)) */
  def findUpdateInPlace[A](items: M[A], predicate: A => Boolean, newItem: A): Boolean = {
    // #Warning: This implementation assumes cheap O(1) index access.
    //  Override this method for efficiency as needed (e.g. for ListBuffer – see below).
    var found = false
    var index = 0
    val len = size(items)
    while (!found && index < len) {
      if (predicate(getByIndex(items, index))) {
        updateAtIndex(items, index, newItem)
        found = true
      }
      index += 1
    }
    found
  }
}

object MutableSplittable {

  implicit object JsArrayMutableSplittable extends MutableSplittable[JsArray] {

    override val splittable: Splittable[JsArray] = Splittable.JsArraySplittable

    override def size[A](items: JsArray[A]): Int = items.length

    override def getByIndex[A](items: JsArray[A], index: Int): A = items(index)

    override def updateAtIndex[A](items: JsArray[A], index: Int, newItem: A): Unit = {
      items.update(index, newItem)
    }
  }

  implicit object ScalaJsArrayMutableSplittable extends MutableSplittable[js.Array] {

    override val splittable: Splittable[js.Array] = Splittable.ScalaJsArraySplittable

    override def size[A](items: js.Array[A]): Int = items.length

    override def getByIndex[A](items: js.Array[A], index: Int): A = items(index)

    override def updateAtIndex[A](items: js.Array[A], index: Int, newItem: A): Unit = {
      items.update(index, newItem)
    }
  }

  implicit object BufferMutableSplittable extends MutableSplittable[mutable.Buffer] {

    override val splittable: Splittable[mutable.Buffer] = Splittable.BufferSplittable

    override def size[A](items: mutable.Buffer[A]): Int = items.size

    override def getByIndex[A](items: mutable.Buffer[A], index: Int): A = items(index)

    override def updateAtIndex[A](items: mutable.Buffer[A], index: Int, newItem: A): Unit = {
      items.update(index, newItem)
    }

    override def findUpdateInPlace[A](
      items: mutable.Buffer[A],
      predicate: A => Boolean,
      newItem: A
    ): Boolean = {
      items match {
        case _: mutable.ListBuffer[A @unchecked] =>
          // This implementation is more efficient when accessing elements by index is costly.
          val iterator = items.iterator
          var found = false
          var index = 0
          while (!found && iterator.hasNext) {
            if (predicate(iterator.next())) {
              updateAtIndex(items, index, newItem)
              found = true
            }
            index += 1
          }
          found
        case _ =>
          // All other Buffer implementations are indexed (I think)
          super.findUpdateInPlace(items, predicate, newItem)
      }
    }
  }
}
