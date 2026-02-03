package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import com.raquo.airstream.distinct.DistinctOps
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny}

import scala.annotation.compileTimeOnly

final case class SplitMatchSeqObservable[Self[+_] <: Observable[_] , I, K, O, CC[_]] private (private val underlying: Unit) extends AnyVal

object SplitMatchSeqObservable {

  @compileTimeOnly("`splitMatchSeq` without `toSignal` is illegal")
  def build[Self[+_] <: Observable[_] , I, K, O, CC[_]](
    keyFn: Function1[I, K],
    // distinctCompose: Function1[KeyedStrictSignal[K, I], KeyedStrictSignal[K, I]],
    distinctCompose: DistinctOps.DistinctorF[I], // Function1[KeyedStrictSignal[K, I], KeyedStrictSignal[K, I]],
    duplicateKeysConfig: DuplicateKeysConfig,
    observable: BaseObservable[Self, CC[I]]
  )(
    caseList: CaseAny*
  )(
    handleList: HandlerAny[O]*
  ): SplitMatchSeqObservable[Self, I, K, O, CC] = throw new UnsupportedOperationException("`splitMatchSeq` without `toSignal` is illegal")

}
