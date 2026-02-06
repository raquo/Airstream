package com.raquo.airstream.split

import com.raquo.airstream.core.{BaseObservable, Observable}
import com.raquo.airstream.distinct.DistinctOps.DistinctOp
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, MatchValueHandler}

import scala.annotation.compileTimeOnly

final case class SplitMatchSeqValueObservable[Self[+_] <: Observable[_], I, K, O, CC[_], V] private (private val underlying: Unit) extends AnyVal

object SplitMatchSeqValueObservable {

  @compileTimeOnly("`splitMatchSeq` without `toSignal` is illegal")
  def build[Self[+_] <: Observable[_], I, K, O, CC[_], V](
    keyFn: Function1[I, K],
    distinctOp: DistinctOp[I],
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
