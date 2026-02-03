package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, Signal}
import com.raquo.airstream.distinct.DistinctOps

class SplittableObservable[M[_], Input](val observable: Observable[M[Input]]) extends AnyVal {

  // #nc[split] rename distinctCompose to distinctF?

  def splitSeq[Output, Key](
    key: Input => Key,
    // distinctCompose: KeyedStrictSignal[_, Input] => KeyedStrictSignal[_, Input] = _.distinct,
    distinctCompose: DistinctOps.DistinctorF[Input] = _.distinct,
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
      parentSignal, key, distinctCompose, project, splittable, duplicateKeys
    )
  }

  def splitByIndex[Output](
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
      distinctCompose = _.distinctBy(_._1),
      project = signal => project(signal.map(_._1)),
      splittable,
      DuplicateKeysConfig.noWarnings // No need to check for duplicates – we know the keys are good.
    )
  }

  @deprecated("Instead of `.split(...) { (key, initial, signal) => ... }`, use `.split(...)(strictSignal => ...)` – you can now read `.key` and `.now()` from `strictSignal`, but note that `.now()` updates over time, unlike `initial` before.", "18.0.0-M2")
  def split[Output, Key](
    key: Input => Key,
    // distinctCompose: KeyedStrictSignal[_, Input] => KeyedStrictSignal[_, Input] = (_: KeyedStrictSignal[_, Input]).distinct,
    distinctCompose: DistinctOps.DistinctorF[Input] = _.distinct,
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
  @deprecated("Instead of `.splitByIndex { (ix, initial, signal) => ... }`, use `.split(...)(strictSignal => ...)` – you can now read `.key` (containing index) and `.now()` from `strictSignal`, but note that `.now()` updates over time, unlike `initial` before.", "18.0.0-M2")
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
      distinctCompose = _.distinctBy(_._1),
      project = s => project(s.key, s.now()._1, s.map(_._1)),
      splittable,
      DuplicateKeysConfig.noWarnings // No need to check for duplicates – we know the keys are good.
    )
  }
}
