package com.raquo.airstream.core

import com.raquo.airstream.core.{Observable, BaseObservable, Signal}
import com.raquo.airstream.split.*

trait ObservableMacroImplicits {

  extension [Self[+_] <: Observable[_], I](inline observable: BaseObservable[Self, I]) {
    inline def splitMatchOne: SplitMatchOneObservable[Self, I, Nothing] =
      SplitMatchOneObservable.build(observable)()()
  }

  extension [Self[+_] <: Observable[_], I, O](
    inline matchSplitObservable: SplitMatchOneObservable[Self, I, O]
  ) {
    inline def handleCase[A, B, O1 >: O](inline casePf: PartialFunction[A, B])(inline handleFn: (B, Signal[B]) => O1): SplitMatchOneObservable[Self, I, O1] =
      SplitMatchOneMacros.deglateHandleCase(matchSplitObservable, casePf, handleFn)

    inline def handleType[T]: SplitMatchOneTypeObservable[Self, I, O, T] = 
      SplitMatchOneMacros.deglateHandleType(matchSplitObservable)

    inline def handleValue[V](inline v: V)(using inline valueOf: ValueOf[V]): SplitMatchOneValueObservable[Self, I, O, V] =
      SplitMatchOneMacros.deglateHandleValue(matchSplitObservable, v)
  }

  extension [Self[+_] <: Observable[_], I, O, T](inline matchTypeObserver: SplitMatchOneTypeObservable[Self, I, O, T]) {
    inline def apply[O1 >: O](inline handleFn: (T, Signal[T]) => O1): SplitMatchOneObservable[Self, I, O1] =
      SplitMatchOneMacros.deglateHandleTypeApply(matchTypeObserver, handleFn)
  }

  extension [Self[+_] <: Observable[_], I, O, V](inline matchValueObservable: SplitMatchOneValueObservable[Self, I, O, V]) {
    inline def apply[O1 >: O](inline handle: => O1): SplitMatchOneObservable[Self, I, O1] =
      SplitMatchOneMacros.deglateHandleValueApply(matchValueObservable, (_, _) => handle)
  }

  extension [I, O](inline matchSplitObservable: SplitMatchOneObservable[Signal, I, O]) {
    inline def toSignal: Signal[O] = SplitMatchOneMacros.deglateToSignal(matchSplitObservable)
  }

  extension [I, O](inline matchSplitObservable: SplitMatchOneObservable[EventStream, I, O]) {
    inline def toStream: EventStream[O] = SplitMatchOneMacros.deglateToStream(matchSplitObservable)
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

  extension [Self[+_] <: Observable[_], I, K, O, CC[_]](
    inline matchSplitObservable: SplitMatchSeqObservable[Self, I, K, O, CC]
  ) {
    inline def handleCase[A, B, O1 >: O](inline casePf: PartialFunction[A, B])(inline handleFn: (B, Signal[B]) => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] =
      SplitMatchSeqMacros.deglateHandleCase(matchSplitObservable, casePf, handleFn)

    inline def handleType[T]: SplitMatchSeqTypeObservable[Self, I, K, O, CC, T] =
      SplitMatchSeqMacros.deglateHandleType(matchSplitObservable)

    inline def handleValue[V](inline v: V)(using inline valueOf: ValueOf[V]): SplitMatchSeqValueObservable[Self, I, K, O, CC, V] =
      SplitMatchSeqMacros.deglateHandleValue(matchSplitObservable, v)

    inline def toSignal: Signal[CC[O]] =
      SplitMatchSeqMacros.deglateToSignal(matchSplitObservable)
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_], T](inline matchTypeObserver: SplitMatchSeqTypeObservable[Self, I, K, O, CC, T]) {
    inline def apply[O1 >: O](inline handleFn: (T, Signal[T]) => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] =
      SplitMatchSeqMacros.deglateHandleTypeApply(matchTypeObserver, handleFn)
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_], V](inline matchValueObservable: SplitMatchSeqValueObservable[Self, I, K, O, CC, V]) {

    inline def apply[O1 >: O](inline handle: => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] =
      SplitMatchSeqMacros.deglateHandleValueApply(matchValueObservable, (_, _) => handle)
  }
}
