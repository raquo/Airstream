package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}

class SplittableOneStream[Input](val stream: EventStream[Input]) extends AnyVal {

  /** This operator works like `split`, but with only one item, not a collection of items. */
  def splitOne[Output, Key](
    key: Input => Key
  )(
    project: (Key, Input, Signal[Input]) => Output
  ): EventStream[Output] = {
    // @TODO[Performance] Would be great if we didn't need .toWeakSignal and .map, but I can't figure out how to do that
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    new SplittableSignal(stream.toWeakSignal)
      .split(
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
    project: (Input, Signal[Input]) => Output,
    startWith: Output
  ): Signal[Output] = {
    stream.toWeakSignal.splitOption(project, startWith)
  }

}
