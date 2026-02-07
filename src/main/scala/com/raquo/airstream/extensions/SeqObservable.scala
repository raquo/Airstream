package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.split.Splittable

class SeqObservable[A, Self[+_] <: Observable[_], M[_]](
  private val observable: BaseObservable[Self, M[A]]
) extends AnyVal {

  /** Maps the value in Some(x) */
  def mapSeq[B](project: A => B)(implicit splittable: Splittable[M]): Self[M[B]] = {
    observable.map { seq =>
      splittable.map(seq, project)
    }
  }

  /** If the Seq-like M[A] in this observable is empty, map to M containing `ifEmpty` instead. */
  def seqOrElse(ifEmpty: => A)(implicit splittable: Splittable[M]): Self[M[A]] =
    observable.map { seq =>
      if (splittable.isEmpty(seq)) {
        splittable.create(ifEmpty :: Nil)
      } else {
        seq
      }
    }

  // #TODO Try to implement seqOrElseSeq where ifEmpty is M[A] instead of A
  //  - hard to get implicit right for this
  //  - we could potentially use a concrete Seq but then we'd need Splittable to have fromSeq

}
