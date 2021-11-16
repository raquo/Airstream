package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}

class SplittableOneEventStream[Input](val stream: EventStream[Input]) extends AnyVal {

  def splitOne[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct
  )(
    project: (Key, Input, Signal[Input]) => Output
  ): EventStream[Output] = {
    // @TODO[Performance] Would be great if we didn't need .toWeakSignal and .map, but I can't figure out how to do that
    new SplittableSignal(stream.toWeakSignal)
      .split(key, distinctCompose)(project)
      .changes
      .map(_.get)
  }
}
