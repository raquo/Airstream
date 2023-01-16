package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}

class SplittableStream[M[_], Input](val stream: EventStream[M[Input]]) extends AnyVal {

  def split[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: (Key, Input, Signal[Input]) => Output
  )(
    implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, Input, Output, Key](
      parent = stream.startWith(splittable.empty),
      key,
      distinctCompose,
      project,
      splittable,
      duplicateKeys
    )
  }

  /** Like `split`, but uses index of the item in the list as the key. */
  def splitByIndex[Output](
    project: (Int, Input, Signal[Input]) => Output
  )(
    implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, (Input, Int), Output, Int](
      parent = stream.map(splittable.zipWithIndex).startWith(splittable.empty),
      key = _._2, // Index
      distinctCompose = _.distinctBy(_._1),
      project = (index: Int, initialTuple, tupleSignal) => {
        project(index, initialTuple._1, tupleSignal.map(_._1))
      },
      splittable,
      DuplicateKeysConfig.noWarnings  // No need to check for duplicates – we know the keys are good.
    )
  }
}
