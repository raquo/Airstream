package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly
import com.raquo.airstream.core.Signal

final case class SplitMatchSeqObservable[Self[+_] <: Observable[_] , I, K, O, CC[_]] private (private val underlying: Unit) extends AnyVal

object SplitMatchSeqObservable {

  @compileTimeOnly("`splitMatchSeq` without `toSignal` is illegal")
  def build[Self[+_] <: Observable[_] , I, K, O, CC[_]](
    keyFn: Function1[I, K],
    distinctCompose: Function1[Signal[I], Signal[I]],
    duplicateKeysConfig: DuplicateKeysConfig,
    observable: BaseObservable[Self, CC[I]],
    caseList: List[PartialFunction[Any, Any]],
    handlerMap: Map[Int, Function2[Any, Any, O]]
  ): SplitMatchSeqObservable[Self, I, K, O, CC] = throw new UnsupportedOperationException("`splitMatchSeq` without `toSignal` is illegal")

}
