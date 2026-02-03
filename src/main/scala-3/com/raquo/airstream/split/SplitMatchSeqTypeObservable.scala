package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import com.raquo.airstream.distinct.DistinctOps
import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, MatchTypeHandler}

import scala.annotation.compileTimeOnly

final case class SplitMatchSeqTypeObservable[Self[+_] <: Observable[_] , I, K, O, CC[_], T] private (private val underlying: Unit) extends AnyVal

object SplitMatchSeqTypeObservable {

  @compileTimeOnly("`splitMatchSeq` without `toSignal` is illegal")
  def build[Self[+_] <: Observable[_] , I, K, O, CC[_], T](
    keyFn: Function1[I, K],
    // distinctCompose: Function1[Signal[I], Signal[I]],
    distinctCompose: DistinctOps.DistinctorF[I],
    duplicateKeysConfig: DuplicateKeysConfig,
    observable: BaseObservable[Self, CC[I]]
  )(
    caseList: CaseAny*
  )(
    handleList: HandlerAny[O]*
  )(
    tHandler: MatchTypeHandler[T]
  ): SplitMatchSeqTypeObservable[Self, I, K, O, CC, T] = throw new UnsupportedOperationException("`splitMatchSeq` without `toSignal` is illegal")

}
