package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.StrictSignal

/** See also: [[SplittableOneObservable]] */
class SplittableOneStream[Input](
  private val stream: EventStream[Input]
) extends AnyVal {

  /** This operator lets you "split" EventStream[Input] into two branches:
    *  - processing of Signal[Input] into Output, and
    *  - the initial value of Output.
    * This is a nice shorthand to signal.splitOption in cases
    * when signal is actually stream.toWeakSignal or stream.startWith(initial)
    */
  def splitStart[Output](
    project: StrictSignal[Input] => Output,
    startWith: Output
  ): Signal[Output] = {
    stream.toWeakSignal.splitOption(project, startWith)
  }

}
