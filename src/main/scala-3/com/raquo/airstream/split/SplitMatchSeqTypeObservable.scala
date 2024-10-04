package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly
import com.raquo.airstream.core.Signal

final case class SplitMatchSeqTypeObservable[Self[+_] <: Observable[_] , I, K, O, CC[_], T] private (private val underlying: Unit) extends AnyVal

object SplitMatchSeqTypeObservable {

  @compileTimeOnly("`splitMatchSeq` without `toSignal` is illegal")
  def build[Self[+_] <: Observable[_] , I, K, O, CC[_], T](
    keyFn: Function1[I, K],
    distinctCompose: Function1[Signal[I], Signal[I]],
    duplicateKeysConfig: DuplicateKeysConfig,
    observable: BaseObservable[Self, CC[I]],
    caseList: List[PartialFunction[Any, Any]],
    handlerMap: Map[Int, Function2[Any, Any, O]],
    tCast: PartialFunction[T, T]
  ): SplitMatchSeqTypeObservable[Self, I, K, O, CC, T] = throw new UnsupportedOperationException("`splitMatchSeq` without `toSignal` is illegal")

}
