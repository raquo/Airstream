package com.raquo.airstream

import com.raquo.ew.{JsArray, JsSet}

import scala.util.{Failure, Try}

package object util {

  type Id[A] = A

  implicit class IdWrap[M[_], A](private val container: M[A]) extends AnyVal {
    def idWrap: M[Id[A]] = container
  }

  implicit class IdUnwrap[M[_], A](private val container: M[Id[A]]) extends AnyVal {
    def idUnwrap: M[A] = container
  }

  val always: Any => Boolean = _ => true

  /** Returns true if two or more of the `items` map to the same key.
    *
    * Note: `@inline` should eliminate the allocation of `key` lambda.
    *
    * #Note: keys are compared using JS `===` semantics, i.e. by reference
    *  for objects, NOT using Scala `equals`. Only use this for identity-keyed
    *  checks such as Var / WriteBus de-duplication.
    */
  @inline def hasDuplicateKeys[A](items: Seq[A])(key: A => Any): Boolean = {
    val seenKeys = JsSet.empty[Any]
    items.foreach { item =>
      seenKeys.add(key(item))
    }
    items.size != seenKeys.size
  }

  /** Removes the item at `index` (must be a valid index) from `array`.
    *
    * For the first item we use `shift()`, which V8 does in O(1) by moving the array's
    * start (except for very large arrays), whereas `splice(0, 1)` moves every other item.
    * This makes removing many items in insertion order – e.g. when unmounting a long
    * list, observers and subscriptions go away oldest-first – linear, not quadratic.
    */
  @inline private[airstream] def removeAt[A](array: JsArray[A], index: Int): Unit = {
    if (index == 0) {
      val _ = array.shift()
    } else {
      val _ = array.splice(index, deleteCount = 1)
    }
  }

  /** Like `Try(v).flatten`, but avoids allocating another `Success`. */
  def tryOrFailure[A](v: => Try[A]): Try[A] = {
    try {
      v
    } catch {
      case err: Throwable => Failure(err)
    }
  }
}
