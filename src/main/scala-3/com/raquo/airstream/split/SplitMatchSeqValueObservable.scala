package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import com.raquo.airstream.distinct.DistinctOps
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, MatchValueHandler}

import scala.annotation.compileTimeOnly

final case class SplitMatchSeqValueObservable[Self[+_] <: Observable[_] , I, K, O, CC[_], V] private (private val underlying: Unit) extends AnyVal

object SplitMatchSeqValueObservable {

  @compileTimeOnly("`splitMatchSeq` without `toSignal` is illegal")
  def build[Self[+_] <: Observable[_] , I, K, O, CC[_], V](
    keyFn: Function1[I, K],
    // distinctCompose: Function1[KeyedStrictSignal[K, I], KeyedStrictSignal[K, I]],
    distinctCompose:  DistinctOps.DistinctorF[I],
    duplicateKeysConfig: DuplicateKeysConfig,
    observable: BaseObservable[Self, CC[I]]
  )(
    caseList: CaseAny*
  )(
    handleList: HandlerAny[O]*
  )(
    valueHandler: MatchValueHandler[V]
  ): SplitMatchSeqValueObservable[Self, I, K, O, CC, V] = throw new UnsupportedOperationException("`splitMatchSeq` without `toSignal` is illegal")

}
