package com.raquo.airstream

package object util {

  type Id[A] = A

  implicit class IdWrap[M[_], A](private val container: M[A]) extends AnyVal {
    def idWrap: M[Id[A]] = container
  }

  implicit class IdUnwrap[M[_], A](private val container: M[Id[A]]) extends AnyVal {
    def idUnwrap: M[A] = container
  }

  val always: Any => Boolean = _ => true

  def hasDuplicateTupleKeys(tuples: Seq[(_, _)]): Boolean = {
    tuples.size != tuples.map(_._1).toSet.size
  }
}
