package com.raquo.airstream.split

import com.raquo.airstream.core.Signal

class SplittableSignal[M[_], Input](val signal: Signal[M[Input]]) extends AnyVal {

  def split[Output, Key](
    key: Input => Key,
    distinctCompose: Signal[Input] => Signal[Input] = _.distinct
  )(
    project: (Key, Input, Signal[Input]) => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitSignal[M, Input, Output, Key](
      parent = signal,
      key = key,
      distinctCompose = distinctCompose,
      project = project,
      splittable
    )
  }
}
