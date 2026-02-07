package com.raquo.airstream.split

import com.raquo.airstream.core.Signal
import com.raquo.airstream.distinct.DistinctOps.DistinctOp
import com.raquo.airstream.state.Var

class SplittableVar[M[_], Input](
  private val v: Var[M[Input]]
) extends AnyVal {

  /** This `splitSeq` operator works on Vars. In contrast to the regular `splitSeq` operator,
    * it provides you with a writable Var for each item, not just a Signal for each item.
    *
    * This makes it easy to update the individual item in the parent Var from within
    * the item's context.
    */
  def splitSeq[Output, Key](
    key: Input => Key,
    distinctOp: DistinctOp[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: KeyedDerivedVar[Key, M[Input], Input] => Output
  )(implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, Input, Output, Key](
      parent = v.signal,
      key,
      distinctOp,
      project = signal => {
        val thisKey = signal.key
        val childVar = new KeyedDerivedVar[Key, M[Input], Input](
          parent = v,
          signal = signal,
          key = thisKey,
          updateParent = KeyedDerivedVar.standardErrorsF { (inputs, newInput) =>
            Some(splittable.findUpdate(inputs, key(_) == thisKey, newInput))
          },
          displayNameSuffix = s".splitSeq(key = ${key})"
        )
        project(childVar)
      },
      splittable,
      duplicateKeys
    )
  }

  /** Like `split`, but uses index of the item in the list as the key. */
  def splitSeqByIndex[Output](
    project: KeyedDerivedVar[Int, M[Input], Input] => Output
  )(implicit splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, (Input, Int), Output, Int](
      parent = v.signal.map(splittable.zipWithIndex),
      key = _._2, // Index
      distinctOp = _.distinctBy(_._1),
      project = tupleSignal => {
        val thisIndex = tupleSignal.key
        val childVar = new KeyedDerivedVar[Int, M[Input], Input](
          parent = v,
          signal = tupleSignal.map(_._1),
          key = thisIndex,
          updateParent = KeyedDerivedVar.standardErrorsF { (inputs, newInput) =>
            Some(splittable.findUpdateByIndex(inputs, thisIndex, newInput))
          },
          displayNameSuffix = s".splitSeqByIndex(index = ${thisIndex})"
        )
        project(childVar)
      },
      splittable,
      DuplicateKeysConfig.noWarnings, // No need to check for duplicates – we know the keys are good.?
    )
  }

  /** This variation of the `splitSeq` operator is designed for Var-s of
    * mutable collections. It works like the usual split, except that
    * it updates the mutable collection in-place instead of creating
    * a modified copy of it, like the regular `split` operator does.
    *
    * Note that the regular `splitSeq` operators work fine with both mutable
    * and immutable collections, treating both of them as immutable.
    */
  def splitSeqMutate[Output, Key](
    key: Input => Key,
    distinctOp: DistinctOp[Input] = _.distinct,
    duplicateKeys: DuplicateKeysConfig = DuplicateKeysConfig.default
  )(
    project: KeyedDerivedVar[Key, M[Input], Input] => Output
  )(implicit splittable: MutableSplittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, Input, Output, Key](
      parent = v.signal,
      key,
      distinctOp,
      project = signal => {
        val thisKey = signal.key
        val childVar = new KeyedDerivedVar[Key, M[Input], Input](
          parent = v,
          signal = signal,
          key = thisKey,
          updateParent = KeyedDerivedVar.standardErrorsF { (inputs, newInput) =>
            splittable.findUpdateInPlace[Input](inputs, key(_) == thisKey, newInput)
            Some(inputs)
          },
          displayNameSuffix = s".splitMutate(key = ${key})"
        )
        project(childVar)
      },
      splittable.splittable,
      duplicateKeys,
    )
  }
}
