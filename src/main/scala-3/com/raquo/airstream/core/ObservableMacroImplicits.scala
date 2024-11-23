package com.raquo.airstream.core

import com.raquo.airstream.core.{Observable, BaseObservable, Signal}
import com.raquo.airstream.split.{DuplicateKeysConfig, SplitMatchOneObservable, SplitMatchSeqObservable}

trait ObservableMacroImplicits {

  extension [Self[+_] <: Observable[_], I](inline observable: BaseObservable[Self, I]) {
    inline def splitMatchOne: SplitMatchOneObservable[Self, I, Nothing] =
      SplitMatchOneObservable.build(observable)()()
  }

  extension [Self[+_] <: Observable[_], I, K, CC[_]](inline observable: BaseObservable[Self, CC[I]]) {
    inline def splitMatchSeq(
      inline keyFn: Function1[I, K],
      inline distinctCompose: Function1[Signal[I], Signal[I]] = (iSignal: Signal[I]) => iSignal.distinct,
      inline duplicateKeysConfig: DuplicateKeysConfig = DuplicateKeysConfig.default,
    ) = {
      SplitMatchSeqObservable.build(keyFn, distinctCompose, duplicateKeysConfig, observable)()()
    }
  }
}
