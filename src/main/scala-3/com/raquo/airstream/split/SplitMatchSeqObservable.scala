package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly
import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny}

final case class SplitMatchSeqObservable[Self[+_] <: Observable[_] , I, K, O, CC[_]] private (private val underlying: Unit) extends AnyVal

object SplitMatchSeqObservable {

  @compileTimeOnly("`splitMatchSeq` without `toSignal` is illegal")
  def build[Self[+_] <: Observable[_] , I, K, O, CC[_]](
    keyFn: Function1[I, K],
    distinctCompose: Function1[Signal[I], Signal[I]],
    duplicateKeysConfig: DuplicateKeysConfig,
    observable: BaseObservable[Self, CC[I]]
  )(
    caseList: CaseAny*
  )(
    handleList: HandlerAny[O]*
  ): SplitMatchSeqObservable[Self, I, K, O, CC] = throw new UnsupportedOperationException("`splitMatchSeq` without `toSignal` is illegal")

}
