package com.raquo.airstream.split

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.util.IdWrap

/** See also: [[SplittableOneStream]] for additional stream-specific operators */
class SplittableOneObservable[Self[+_] <: Observable[_], Input](
  private val observable: BaseObservable[Self, Input]
) extends AnyVal {

  /** This operator works like `split`, but with only one item, not a collection of items. */
  def splitOne[Output, Key](
    key: Input => Key
  )(
    project: KeyedStrictSignal[Key, Input] => Output
  ): Self[Output] = {
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    observable.matchStreamOrSignalAsSelf(
      ifStream = { stream =>
        // @TODO[Performance] Would be great if we didn't need .toWeakSignal and .map, but I can't figure out how to do that
        //  - We now have unsafeIdSplittable, but splitSeq would need to evaluate its `empty` value, which throws.
        stream.toWeakSignal
          .splitSeq(
            key,
            distinctOp = identity,
            DuplicateKeysConfig.noWarnings
          )(
            project
          )
          .changes
          .map(_.get)
      },
      ifSignal = { signal =>
        signal
          .idWrap // Signal[A] => Signal[Id[A]] type helper
          .splitSeq(
            key,
            distinctOp = identity,
            DuplicateKeysConfig.noWarnings
          )(
            project
          )(
            Splittable.unsafeIdSplittable // #Safe because Signal.splitSeq does not use Splittable.empty
          )
      }
    )
  }
}
