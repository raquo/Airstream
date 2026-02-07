package com.raquo.airstream.split

import com.raquo.airstream.core.{BaseObservable, Observable, Signal}
import com.raquo.airstream.distinct.DistinctOps.DistinctOp

class SplittableSeqObservable[Self[+_] <: Observable[_], M[_], Input](
  private val observable: BaseObservable[Self, M[Input]]
) extends AnyVal {

  def splitSeq[Output, Key](
    key: Input => Key,
    distinctOp: DistinctOp[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: KeyedStrictSignal[Key, Input] => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[M[Output]] = {
    val parentSignal = observable.toSignalIfStream(
      ifStream = _.startWith(splittable.empty, cacheInitialValue = true)
    )
    new SplitSignal[M, Input, Output, Key](
      parentSignal, key, distinctOp, project, splittable, duplicateKeys
    )
  }

  /** Like splitSeq, but works on `Option[Seq[Input]]`` instead of `Seq[Input]`, treating `None` as empty Seq. */
  def splitSomeSeq[Output, Key](
    key: Input => Key,
    distinctOp: DistinctOp[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: KeyedStrictSignal[Key, Input] => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[M[Output]] = {
    observable
      .map { seq =>
        if (splittable.isEmpty(seq)) {
          splittable.empty
        } else {
          seq
        }
      }
      .asInstanceOf[Observable[M[Input]]] // #nc Compiler knows that Self[A] is an Observable[_] but not that it's an Observable[A] – why?
      .splitSeq(
        key, distinctOp, duplicateKeys
      )(
        project
      )
  }

  /** Like `splitSeq`, but uses index of the item in the list as the key. */
  def splitSeqByIndex[Output](
    project: KeyedStrictSignal[Int, Input] => Output
  )(implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    val parentSignal = {
      observable
        .toSignalIfStream(_.startWith(splittable.empty, cacheInitialValue = true))
        .map(splittable.zipWithIndex)
    }
    new SplitSignal[M, (Input, Int), Output, Int](
      parent = parentSignal,
      key = _._2, // Index
      distinctOp = _.distinctBy(_._1),
      project = signal => project(signal.map(_._1)),
      splittable,
      DuplicateKeysConfig.noWarnings // No need to check for duplicates – we know the keys are good.
    )
  }

  @deprecated("Instead of `.split(...) { (key, initial, signal) => ... }`, use `.splitSeq(...)(strictSignal => ...)` – you can now read `.key` and `.now()` from `strictSignal`, but note that `.now()` updates over time, unlike `initial` before.", "18.0.0-M3")
  def split[Output, Key](
    key: Input => Key,
    distinctCompose: DistinctOp[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: (Key, Input, Signal[Input]) => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[M[Output]] = {
    val parentSignal = observable.toSignalIfStream(
      ifStream = _.startWith(splittable.empty, cacheInitialValue = true)
    )
    new SplitSignal[M, Input, Output, Key](
      parentSignal, key, distinctCompose, s => project(s.key, s.now(), s), splittable, duplicateKeys
    )
  }

  /** Like `split`, but uses index of the item in the list as the key. */
  @deprecated("Instead of `.splitByIndex { (ix, initial, signal) => ... }`, use `.splitSeqByIndex(...)(strictSignal => ...)` – you can now read `.key` (containing index) and `.now()` from `strictSignal`, but note that `.now()` updates over time, unlike `initial` before.", "18.0.0-M3")
  def splitByIndex[Output](
    project: (Int, Input, Signal[Input]) => Output
  )(implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    val parentSignal = {
      observable
        .toSignalIfStream(_.startWith(splittable.empty, cacheInitialValue = true))
        .map(splittable.zipWithIndex)
    }
    new SplitSignal[M, (Input, Int), Output, Int](
      parent = parentSignal,
      key = _._2, // Index
      distinctOp = _.distinctBy(_._1),
      project = s => project(s.key, s.now()._1, s.map(_._1)),
      splittable,
      DuplicateKeysConfig.noWarnings // No need to check for duplicates – we know the keys are good.
    )
  }
}
