package com.raquo.airstream.split

import com.raquo.airstream.core.Signal

class SplittableOneSignal[Input](val signal: Signal[Input]) extends AnyVal {

  def splitOne[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct
  )(
    project: (Key, Input, Signal[Input]) => Output
  ): Signal[Output] = {
    // @TODO[Performance] Would be great if we didn't need two .map-s, but I can't figure out how to do that
    new SplittableSignal(signal.map(Some(_): Option[Input]))
      .split(key, distinctCompose)(project)
      .map(_.get)
  }
}
