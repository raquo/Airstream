package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, Signal}

class SplittableObservable[M[_], Input](val observable: Observable[M[Input]]) extends AnyVal {

  def split[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct,
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
      parentSignal, key, distinctCompose, project, splittable, duplicateKeys
    )
  }

  /** Like `split`, but uses index of the item in the list as the key. */
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
      project = (index: Int, initialTuple, tupleSignal) => {
        project(index, initialTuple._1, tupleSignal.map(_._1))
      },
      splittable,
      DuplicateKeysConfig.noWarnings // No need to check for duplicates – we know the keys are good.
    )
  }
}
