package com.raquo.airstream

package object util {

  type Id[A] = A

  def hasDuplicateTupleKeys[K[_], A](tuples: Seq[(K[A], _)]): Boolean = {
    tuples.size != tuples.map(_._1).toSet.size
  }
}
