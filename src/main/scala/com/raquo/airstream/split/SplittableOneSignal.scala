package com.raquo.airstream.split

import com.raquo.airstream.core.Signal
import com.raquo.airstream.util.IdWrap

class SplittableOneSignal[Input](val signal: Signal[Input]) extends AnyVal {

  /** This operator works like `split`, but with only one item, not a collection of items. */
  def splitOne[Output, Key](
    key: Input => Key
  )(
    project: KeyedStrictSignal[Key, Input] => Output
  ): Signal[Output] = {
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    signal
      .idWrap // Signal[A] => Signal[Id[A]] type helper
      .splitSeq(
        key,
        distinctCompose = identity,
        DuplicateKeysConfig.noWarnings
      )(
        project
      )(
        Splittable.unsafeIdSplittable // #Safe because Signal.splitSeq does not use Splittable.empty
      )
  }
}
