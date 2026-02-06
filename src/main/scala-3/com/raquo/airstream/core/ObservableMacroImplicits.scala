package com.raquo.airstream.core

import com.raquo.airstream.core.{Observable, BaseObservable, Signal}
import com.raquo.airstream.distinct.DistinctOps
import com.raquo.airstream.distinct.DistinctOps.DistinctOp
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
      SplitMatchOneMacros.delegateHandleCase(matchSplitObservable, casePf, handleFn)

    inline def handleType[T]: SplitMatchOneTypeObservable[Self, I, O, T] =
      SplitMatchOneMacros.delegateHandleType(matchSplitObservable)

    inline def handleValue[V](inline v: V)(using inline valueOf: ValueOf[V]): SplitMatchOneValueObservable[Self, I, O, V] =
      SplitMatchOneMacros.delegateHandleValue(matchSplitObservable, v)
  }

  extension [Self[+_] <: Observable[_], I, O, T](inline matchTypeObserver: SplitMatchOneTypeObservable[Self, I, O, T]) {
    inline def apply[O1 >: O](inline handleFn: (T, Signal[T]) => O1): SplitMatchOneObservable[Self, I, O1] =
      SplitMatchOneMacros.delegateHandleTypeApply(matchTypeObserver, handleFn)
  }

  extension [Self[+_] <: Observable[_], I, O, V](inline matchValueObservable: SplitMatchOneValueObservable[Self, I, O, V]) {
    inline def apply[O1 >: O](inline handle: => O1): SplitMatchOneObservable[Self, I, O1] =
      SplitMatchOneMacros.delegateHandleValueApply(matchValueObservable, (_, _) => handle)
  }

  extension [I, O](inline matchSplitObservable: SplitMatchOneObservable[Signal, I, O]) {
    inline def toSignal: Signal[O] = SplitMatchOneMacros.delegateToSignal(matchSplitObservable)
  }

  extension [I, O](inline matchSplitObservable: SplitMatchOneObservable[EventStream, I, O]) {
    inline def toStream: EventStream[O] = SplitMatchOneMacros.delegateToStream(matchSplitObservable)
  }

  extension [Self[+_] <: Observable[_], I, K, CC[_]](inline observable: BaseObservable[Self, CC[I]]) {
    inline def splitMatchSeq(
      inline keyFn: Function1[I, K],
      inline distinctOp: DistinctOp[I] = (ops: DistinctOps.Ops[I]) => ops.distinct,
      inline duplicateKeysConfig: DuplicateKeysConfig = DuplicateKeysConfig.default,
    ) = {
      SplitMatchSeqObservable.build(keyFn, distinctOp, duplicateKeysConfig, observable)()()
    }
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_]](
    inline matchSplitObservable: SplitMatchSeqObservable[Self, I, K, O, CC]
  ) {
    inline def handleCase[A, B, O1 >: O](inline casePf: PartialFunction[A, B])(inline handleFn: (B, Signal[B]) => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] =
      SplitMatchSeqMacros.delegateHandleCase(matchSplitObservable, casePf, handleFn)

    inline def handleType[T]: SplitMatchSeqTypeObservable[Self, I, K, O, CC, T] =
      SplitMatchSeqMacros.delegateHandleType(matchSplitObservable)

    inline def handleValue[V](inline v: V)(using inline valueOf: ValueOf[V]): SplitMatchSeqValueObservable[Self, I, K, O, CC, V] =
      SplitMatchSeqMacros.delegateHandleValue(matchSplitObservable, v)

    inline def toSignal: Signal[CC[O]] =
      SplitMatchSeqMacros.delegateToSignal(matchSplitObservable)
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_], T](inline matchTypeObserver: SplitMatchSeqTypeObservable[Self, I, K, O, CC, T]) {
    inline def apply[O1 >: O](inline handleFn: (T, Signal[T]) => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] =
      SplitMatchSeqMacros.delegateHandleTypeApply(matchTypeObserver, handleFn)
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_], V](inline matchValueObservable: SplitMatchSeqValueObservable[Self, I, K, O, CC, V]) {

    inline def apply[O1 >: O](inline handle: => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] =
      SplitMatchSeqMacros.delegateHandleValueApply(matchValueObservable, (_, _) => handle)
  }
}
