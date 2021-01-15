package com.raquo.airstream

package object util {

  type Id[A] = A

  def hasDuplicateTupleKeys(tuples: Seq[(_, _)]): Boolean = {
    tuples.size != tuples.map(_._1).toSet.size
  }
}
