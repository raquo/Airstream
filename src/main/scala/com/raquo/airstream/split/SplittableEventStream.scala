package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}

class SplittableEventStream[M[_], Input](val stream: EventStream[M[Input]]) extends AnyVal {

  def split[Output, Key](
    key: Input => Key)(
    project: (Key, Input, EventStream[Input]) => Output
  )(implicit
    splittable: Splittable[M]
  ): EventStream[M[Output]] = {
    new SplitEventStream[M, Input, Output, Key](
      parent = stream,
      key,
      project,
      splittable
    )
  }

  def splitIntoSignals[Output, Key](
    key: Input => Key)(
    project: (Key, Input, Signal[Input]) => Output
  )(implicit
    splittable: Splittable[M]
  ): EventStream[M[Output]] = {
    new SplitEventStream[M, Input, Output, Key](
      parent = stream,
      key = key,
      project = (key, initialValue, eventStream) => project(key, initialValue, eventStream.toSignal(initialValue)),
      splittable
    )
  }
}
