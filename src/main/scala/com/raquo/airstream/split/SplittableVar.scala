package com.raquo.airstream.split

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.{LazyDerivedVar, LazyStrictSignal, Var}

class SplittableVar[M[_], Input](val v: Var[M[Input]]) extends AnyVal {

  /** This `split` operator works on Vars, and gives you a  */
  def split[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: (Key, Input, Var[Input]) => Output
  )(implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, Input, Output, Key](
      parent = v.signal,
      key,
      distinctCompose,
      project = (thisKey, initial, signal) => {
        val displayNameSuffix = s".split(key = ${key})"
        val childVar = new LazyDerivedVar[M[Input], Input](
          parent = v,
          signal = new LazyStrictSignal[Input, Input](
            signal, identity, signal.displayName, displayNameSuffix + ".signal"
          ),
          zoomOut = (inputs, newInput) => {
            splittable.findUpdate(inputs, key(_) == thisKey, newInput)
          },
          displayNameSuffix = displayNameSuffix
        )
        project(thisKey, initial, childVar)
      },
      splittable,
      duplicateKeys,
      strict = true
    )
  }

  /** Like `split`, but uses index of the item in the list as the key. */
  def splitByIndex[Output](
    project: (Int, Input, Var[Input]) => Output
  )(implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, (Input, Int), Output, Int](
      parent = v.signal.map(splittable.zipWithIndex),
      key = _._2, // Index
      distinctCompose = _.distinctBy(_._1),
      project = (index: Int, initialTuple, tupleSignal) => {
        val displayNameSuffix = s".splitByIndex(index = ${index})"
        val childVar = new LazyDerivedVar[M[Input], Input](
          parent = v,
          signal = new LazyStrictSignal[Input, Input](
            tupleSignal.map(_._1), identity, tupleSignal.displayName, displayNameSuffix + ".signal"
          ),
          zoomOut = (inputs, newInput) => {
            splittable.findUpdateByIndex(inputs, index, newInput)
          },
          displayNameSuffix = displayNameSuffix
        )
        project(index, initialTuple._1, childVar)
      },
      splittable,
      DuplicateKeysConfig.noWarnings, // No need to check for duplicates – we know the keys are good.?
      strict = true
    )
  }

  /** This variation of the `split` operator is designed for Var-s of
    * mutable collections. It works like the usual split, except that
    * it updates the mutable collection in-place instead of creating
    * a modified copy of it, like the regular `split` operator does.
    *
    * Note that the regular `split` operators work fine with both mutable
    * and immutable collections, treating both of them as immutable.
    */
  def splitMutate[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: (Key, Input, Var[Input]) => Output
  )(implicit splittable: MutableSplittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, Input, Output, Key](
      parent = v.signal,
      key,
      distinctCompose,
      project = (thisKey, initial, signal) => {
        val displayNameSuffix = s".splitMutate(key = ${key})"
        val childVar = new LazyDerivedVar[M[Input], Input](
          parent = v,
          signal = new LazyStrictSignal[Input, Input](
            signal, identity, signal.displayName, displayNameSuffix + ".signal"
          ),
          zoomOut = (inputs, newInput) => {
            splittable.findUpdateInPlace[Input](inputs, key(_) == thisKey, newInput)
            inputs
          },
          displayNameSuffix = displayNameSuffix
        )
        project(thisKey, initial, childVar)
      },
      splittable.splittable,
      duplicateKeys,
      strict = true
    )
  }
}
