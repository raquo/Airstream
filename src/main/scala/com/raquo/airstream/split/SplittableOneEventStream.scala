package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.split.Splittable.IdSplittable
import com.raquo.airstream.util.Id

class SplittableOneEventStream[Input](val stream: EventStream[Input]) extends AnyVal {

  def splitOne[Output, Key](
    key: Input => Key)(
    project: (Key, Input, EventStream[Input]) => Output
  ): EventStream[Output] = {
    new SplitEventStream[Id, Input, Output, Key](
      parent = stream,
      key,
      project,
      IdSplittable
    )
  }

  def splitOneIntoSignals[Output, Key](
    key: Input => Key)(
    project: (Key, Input, Signal[Input]) => Output
  ): EventStream[Output] = {
    new SplitEventStream[Id, Input, Output, Key](
      parent = stream,
      key = key,
      project = (key, initialValue, eventStream) => project(key, initialValue, eventStream.toSignal(initialValue)),
      IdSplittable
    )
  }
}
