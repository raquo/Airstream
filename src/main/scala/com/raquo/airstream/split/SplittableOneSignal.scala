package com.raquo.airstream.split

import com.raquo.airstream.core.Signal

class SplittableOneSignal[Input](val signal: Signal[Input]) extends AnyVal {

  /** This operator works like `split`, but with only one item, not a collection of items. */
  def splitOne[Output, Key](
    key: Input => Key
  )(
    project: (Key, Input, Signal[Input]) => Output
  ): Signal[Output] = {
    // @TODO[Performance] Would be great if we didn't need two .map-s, but I can't figure out how to do that
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    new SplittableSignal[Option, Input](signal.map(Some(_)))
      .split(
        key,
        distinctCompose = identity,
        DuplicateKeysConfig.noWarnings
      )(
        project
      )
      .map(_.get)
  }
}
