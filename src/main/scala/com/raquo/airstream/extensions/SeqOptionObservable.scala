package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.split.Splittable

class SeqOptionObservable[A, Self[+_] <: Observable[_], M[_]](
  private val observable: BaseObservable[Self, Option[M[A]]]
) extends AnyVal {

  /** Maps the `A` values inside `Option[M[A]]`, where `M` is `Seq` or a similar type. */
  def mapSeqOpt[B](project: A => B)(implicit splittable: Splittable[M]): Self[Option[M[B]]] = {
    observable.map { opt =>
      opt.map { seq =>
        splittable.map(seq, project)
      }
    }
  }

  /** If the Seq-like M[A] in this observable is empty, map to M containing `ifEmptySeq` instead. */
  def seqOptOrElse(ifEmptySeq: => A)(implicit splittable: Splittable[M]): Self[Option[M[A]]] =
    observable.map { opt =>
      opt.map { seq =>
        if (splittable.isEmpty(seq)) {
          splittable.create(ifEmptySeq :: Nil)
        } else {
          seq
        }
      }
    }

  /** If the Seq-like M[A] in this observable is empty, map to M containing `ifEmptySeq` instead.
    * If the Option is None, map to `ifNone`.
    */
  def seqOrElse(
    ifEmptySeq: => A,
    ifNone: => A
  )(implicit
    splittable: Splittable[M]
  ): Self[M[A]] =
    observable.map { opt =>
      opt.fold(
        splittable.create(ifNone :: Nil)
      ) { seq =>
        if (splittable.isEmpty(seq)) {
          splittable.create(ifEmptySeq :: Nil)
        } else {
          seq
        }
      }
    }
}
