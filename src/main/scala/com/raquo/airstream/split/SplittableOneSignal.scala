package com.raquo.airstream.split

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.Splittable.IdSplittable
import com.raquo.airstream.util.Id

class SplittableOneSignal[Input](val signal: Signal[Input]) extends AnyVal {

  @inline def splitOne[Output, Key](
    key: Input => Key
  )(
    project: (Key, Input, Signal[Input]) => Output
  ): Signal[Output] = {
    splitOneIntoSignals(key)(project)
  }

  def splitOneIntoSignals[Output, Key](
    key: Input => Key
  )(
    project: (Key, Input, Signal[Input]) => Output
  ): Signal[Output] = {
    new SplitEventStream[Id, Input, Output, Key](
      parent = signal.changes,
      key = key,
      project = (key, initialInput, eventStream) => project(key, initialInput, eventStream.toSignal(initialInput)),
      IdSplittable
    ).toSignalWithInitialInput(lazyInitialInput = signal.tryNow())
  }
}
