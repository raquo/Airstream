package com.raquo.airstream

package object util {

  type Id[A] = A

  def hasDuplicateTupleKeys[K](tuples: Seq[(K, _)]): Boolean = {
    tuples.size != tuples.map(_._1).toSet.size
  }
}
