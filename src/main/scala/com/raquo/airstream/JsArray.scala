package com.raquo.airstream

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation._

// @TODO This is a temporary solution for https://github.com/raquo/Laminar/issues/108

/**
  *  Arrays are list-like objects whose prototype has methods to perform
  *  traversal and mutation operations. Neither the length of a JavaScript
  *  array nor the types of its elements are fixed. Since an array's size
  *  length grow or shrink at any time, JavaScript arrays are not guaranteed
  *  to be dense. In general, these are convenient characteristics; but if
  *  these features are not desirable for your particular use, you might
  *  consider using typed arrays.
  *
  *  MDN
  *
  *  To construct a new array with uninitialized elements, use the constructor
  *  of this class. To construct a new array with specified elements, as if
  *  you used the array literal syntax in JavaScript, use the
  *  [[Array$.apply Array.apply]] method instead.
  *
  *  @tparam A Type of the elements of the array
  *
  *  @constructor Creates a new array of length 0.
  */
@js.native
@JSGlobal("Array")
private[raquo] class JsArray[A] extends js.Object {

  //def asIterable: js.Iterable[A] = this.asInstanceOf[js.Iterable[A]]

  /** Creates a new array with the given length.
    *  @param arrayLength Initial length of the array.
    */
  def this(@unused arrayLength: Int) = this()

  // Do not expose this one - use Array(item1, item2, ...) instead
  // def this(items: A*) = this()

  /** Length of the array. */
  def length: Int = js.native

  /** Sets the length of the array.
    *  If the new length is bigger than the old length, created slots are
    *  filled with `undefined` (irrespective of the type argument `A`!).
    *  If the new length is smaller than the old length, the array is shrunk.
    */
  def length_=(@unused v: Int): Unit = js.native

  /** Access the element at the given index. */
  @JSBracketAccess
  def apply(@unused index: Int): A = js.native
  /** Set the element at the given index. */
  @JSBracketAccess
  def update(@unused index: Int, @unused value: A): Unit = js.native

  def map[B](@unused project: js.Function1[A, B]): JsArray[B] = js.native

  def forEach(@unused cb: js.Function1[A, Any]): Unit = js.native

  /**
    * concat creates a new array consisting of the elements in the this object
    * on which it is called, followed in order by, for each argument, the
    * elements of that argument (if the argument is an array) or the argument
    * itself (if the argument is not an array).
    *
    * MDN
    */
  def concat[B >: A](@unused items: JsArray[_ <: B]*): JsArray[B] = js.native

  def indexOf(@unused item: A): Int = js.native

  /**
    * The join() method joins all elements of an array into a string.
    *
    * separator Specifies a string to separate each element of the array.
    * The separator is converted to a string if necessary. If omitted, the
    * array elements are separated with a comma.
    */
  def join(@unused seperator: String = ","): String = js.native

  /**
    * The pop() method removes the last element from an array and returns that
    * element.
    *
    * MDN
    */
  def pop(): A = js.native

  /**
    * The push() method mutates an array by appending the given elements and
    * returning the new length of the array.
    *
    * MDN
    */
  def push(@unused items: A*): Int = js.native

  /**
    * The reverse() method reverses an array in place. The first array element
    * becomes the last and the last becomes the first.
    *
    * MDN
    */
  @JSName("reverse")
  def reverseInPlace(): JsArray[A] = js.native

  /**
    * The shift() method removes the first element from an array and returns that
    * element. This method changes the length of the array.
    *
    * MDN
    */
  def shift(): A = js.native

  /**
    * The slice() method returns a shallow copy of a portion of an array.
    *
    * MDN
    */
  @JSName("slice")
  def jsSlice(@unused start: Int = 0, @unused end: Int = Int.MaxValue): JsArray[A] = js.native

  /**
    * The sort() method sorts the elements of an array in place and returns the
    * array. The sort is not necessarily stable. The default sort order is
    * lexicographic (not numeric).
    *
    * If compareFunction is not supplied, elements are sorted by converting them
    * to strings and comparing strings in lexicographic ("dictionary" or "telephone
    * book," not numerical) order. For example, "80" comes before "9" in
    * lexicographic order, but in a numeric sort 9 comes before 80.
    *
    * MDN
    */
  def sort(@unused compareFn: js.Function2[A, A, Int]): JsArray[A] = js.native

  /** Removes and adds new elements at a given index in the array.
    *
    *  This method first removes `deleteCount` elements starting from the index
    *  `index`, then inserts the new elements `items` at that index.
    *
    *  If `index` is negative, it is treated as that number of elements starting
    *  from the end of the array.
    *
    *  @param index       Index where to start changes
    *  @param deleteCount Number of elements to delete from index
    *  @param items       Elements to insert at index
    *  @return An array of the elements that were deleted
    */
  def splice(@unused index: Int, @unused deleteCount: Int, @unused items: A*): JsArray[A] = js.native

  /**
    * The unshift() method adds one or more elements to the beginning of an array
    * and returns the new length of the array.
    *
    * MDN
    */
  def unshift(@unused items: A*): Int = js.native
}

/** Factory for [[JsArray]] objects. */
@js.native
@JSGlobal("Array")
object JsArray extends js.Object {
  //@js.native
  //@JSGlobal("Array")
  //private object NativeArray extends js.Object {
  //  def isArray(@unused arg: scala.Any): Boolean = js.native
  //  def from[A](@unused iterable: js.Iterable[A]): JsArray[A] = js.native
  //  def apply[A](@unused items: A*): JsArray[A] = js.native
  //}

  /** Creates a new array with the given items. */
  def apply[A](@unused items: A*): JsArray[A] = js.native //NativeArray(items: _*)

  /** Returns true if the given value is an array. */
  def isArray(@unused arg: scala.Any): Boolean = js.native //NativeArray.isArray(arg)

  /** Creates a new array from js.Iterable. */
  def from[A](@unused iterable: js.Iterable[A]): JsArray[A] = js.native //NativeArray.from(iterable)
}
