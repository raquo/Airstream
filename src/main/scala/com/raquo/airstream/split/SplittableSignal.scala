package com.raquo.airstream.split

import com.raquo.airstream.core.Signal

class SplittableSignal[M[_], Input](val signal: Signal[M[Input]]) extends AnyVal {

  // Note: we can't easily have a `splitIntoStreams` on Signal
  //  - the EventStream[Input] argument of `project` would be equivalent to `changes`, missing the initial value
  //  - that strikes me as rather non-obvious, even though we do provide an initial value
  //  - resolving that is actually nasty. You just can't convert a signal into a stream that emits its current value
  //    - the crazy rules about re-emitting values when starting / stopping would be a disservice to everyone
  @inline def split[Output, Key](
    key: Input => Key
  )(
    project: (Key, Input, Signal[Input]) => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[M[Output]] = {
    splitIntoSignals(key)(project)
  }

  def splitIntoSignals[Output, Key](
    key: Input => Key
  )(
    project: (Key, Input, Signal[Input]) => Output
  )(implicit
    splittable: Splittable[M]
  ): Signal[M[Output]] = {
    new SplitEventStream[M, Input, Output, Key](
      parent = signal.changes,
      key = key,
      project = (key, initialInput, eventStream) => project(key, initialInput, eventStream.toSignal(initialInput)),
      splittable
    ).toSignalWithInitialInput(lazyInitialInput = signal.tryNow())
  }
}
