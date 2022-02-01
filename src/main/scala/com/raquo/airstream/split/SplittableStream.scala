package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}

class SplittableStream[M[_], Input](val stream: EventStream[M[Input]]) extends AnyVal {

  def split[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct
  )(
    project: (Key, Input, Signal[Input]) => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, Input, Output, Key](
      parent = stream.startWith(splittable.empty),
      key,
      distinctCompose,
      project,
      splittable
    )
  }
}
