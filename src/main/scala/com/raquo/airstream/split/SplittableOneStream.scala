package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.StrictSignal

class SplittableOneStream[Input](val stream: EventStream[Input]) extends AnyVal {

  /** This operator works like `split`, but with only one item, not a collection of items. */
  def splitOne[Output, Key](
    key: Input => Key
  )(
    project: KeyedStrictSignal[Key, Input] => Output
  ): EventStream[Output] = {
    // @TODO[Performance] Would be great if we didn't need .toWeakSignal and .map, but I can't figure out how to do that
    //  - We now have unsafeIdSplittable, but splitSeq would need to evaluate its `empty` value, which throws.
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    stream.toWeakSignal
      .splitSeq(
        key,
        distinctCompose = identity,
        DuplicateKeysConfig.noWarnings
      )(
        project
      )
      .changes
      .map(_.get)
  }

  /** This operator lets you "split" EventStream[Input] into two branches:
    *  - processing of Signal[Input] into Output, and
    *  - the initial value of Output.
    * This is a nice shorthand to signal.splitOption in cases
    * when signal is actually stream.toWeakSignal or stream.startWith(initial)
    */
  def splitStart[Output](
    project: StrictSignal[Input] => Output,
    startWith: Output
  ): Signal[Output] = {
    stream.toWeakSignal.splitOption(project, startWith)
  }

}
