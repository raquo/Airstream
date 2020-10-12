package com.raquo.airstream

package object util {

  type Id[A] = A

  def hasDuplicateTupleKeys[K[_]](tuples: Seq[(K[_], _)]): Boolean = {
    tuples.size != tuples.map(_._1).toSet.size
  }
}
