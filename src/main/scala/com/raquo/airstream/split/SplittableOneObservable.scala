package com.raquo.airstream.split

import com.raquo.airstream.core.Observable

/** See also: [[SplittableOneSignal]], [[SplittableOneStream]] which
  * provide more specialized output types for `splitOne`.
  */
class SplittableOneObservable[Input](
  private val observable: Observable[Input]
) extends AnyVal {

  /** This operator works like `split`, but with only one item, not a collection of items. */
  def splitOne[Output, Key](
    key: Input => Key
  )(
    project: KeyedStrictSignal[Key, Input] => Output
  ): Observable[Output] = {
    observable.matchStreamOrSignal(
      ifStream = new SplittableOneStream(_).splitOne(key)(project),
      ifSignal = new SplittableOneSignal(_).splitOne(key)(project),
    )
  }
}
