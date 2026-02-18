package com.raquo.airstream.split

import com.raquo.airstream.core.{BaseObservable, Observable, Signal}
import com.raquo.airstream.distinct.DistinctOps.DistinctOp

class SplittableSeqOptionObservable[Self[+_] <: Observable[_], M[_], Input](
  private val observable: BaseObservable[Self, Option[M[Input]]]
) extends AnyVal {

  // #TODO[API] Add Var versions of these.

  /** Like `splitSeq`, but works on the `Seq` inside of `Option[Seq[Input]]`. Leaves the `None` case as-is. */
  def splitSomeSeq[Output, Key](
    key: Input => Key,
    distinctOp: DistinctOp[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: KeyedStrictSignal[Key, Input] => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[Option[M[Output]]] = {
    val parentSignal: Signal[Option[M[Input]]] =
      observable.toSignalIfStream(
        ifStream = _.startWith(None, cacheInitialValue = true)
      )
    new SplitSignal[({type OptionM[V] = Option[M[V]]})#OptionM, Input, Output, Key](
      parentSignal, key, distinctOp, project, splittable.toOptionSplittable, duplicateKeys
    )
  }

  /** Like `splitSeq`, but works on the `Seq` inside of `Option[Seq[Input]]`. Leaves the `None` case as-is. */
  def splitSomeSeqByIndex[Output](
    project: KeyedStrictSignal[Int, Input] => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[Option[M[Output]]] = {
    val parentSignal: Signal[Option[M[(Input, Int)]]] = {
      observable
        .toSignalIfStream(_.startWith(None, cacheInitialValue = true))
        .map(_.map(splittable.zipWithIndex))
    }
    new SplitSignal[({type OptionM[V] = Option[M[V]]})#OptionM, (Input, Int), Output, Int](
      parent = parentSignal,
      key = _._2, // Index
      distinctOp = _.distinctBy(_._1),
      project = signal => project(signal.map(_._1)),
      splittable.toOptionSplittable,
      DuplicateKeysConfig.noWarnings // No need to check for duplicates – we know the keys are good.
    )
  }

}
