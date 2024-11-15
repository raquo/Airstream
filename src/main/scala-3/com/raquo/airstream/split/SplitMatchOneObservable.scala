package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly

/**
 * `MatchSplitObservable` served as macro's data holder for macro expansion.
 *
 * For example:
 *
 * ```scala
 * fooSignal.splitMatch
 *  .handleCase { case Bar(Some(str)) => str } { (str, strSignal) => renderStrNode(str, strSignal) }
 *    .handleCase { case baz: Baz => baz } { (baz, bazSignal) => renderBazNode(baz, bazSignal) }
 * ```
 *
 * will be expanded sematically into:
 *
 * ```scala
 *  MatchSplitObservable.build(fooSignal, ({ case baz: Baz => baz }) :: ({ case Bar(Some(str)) => str }) :: Nil, handlerMap)
 * ```
 */

final case class SplitMatchOneObservable[Self[+_] <: Observable[_] , I, O] private (private val underlying: Unit) extends AnyVal

object SplitMatchOneObservable {

  @compileTimeOnly("`splitMatchOne` without `toSignal`/`toStream` is illegal")
  def build[Self[+_] <: Observable[_] , I, O](
    observable: BaseObservable[Self, I],
    caseList: List[PartialFunction[Any, Any]],
    handlerMap: Map[Int, Function2[Any, Any, O]]
  ): SplitMatchOneObservable[Self, I, O] = throw new UnsupportedOperationException("`splitMatchOne` without `toSignal`/`toStream` is illegal")

}
