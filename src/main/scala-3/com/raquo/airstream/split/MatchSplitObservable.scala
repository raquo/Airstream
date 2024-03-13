package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly

/**
 * `MatchSplitObservable` served as macro's data holder for macro expansion.
 * 
 * For example:
 *
 * ```scala
 *  fooSignal.splitMatch
 *    .handleCase { case Bar(Some(str)) => str } { (str, strSignal) => renderStrNode(str, strSignal) }
 *    .handleCase { case baz: Baz => baz } { (baz, bazSignal) => renderBazNode(baz, bazSignal) }
 * ```
 * 
 * will be expanded sematically into:
 *
 * ```scala
 *  MatchSplitObservable.build(fooSignal, ({ case baz: Baz => baz }) :: ({ case Bar(Some(str)) => str }) :: Nil, handlerMap)
 * ```
 */

opaque type MatchSplitObservable[Self[+_] <: Observable[_] , I, O] = Unit

object MatchSplitObservable {
  
  @compileTimeOnly("splitMatch without toSignal/toStream is illegal")
  def build[Self[+_] <: Observable[_] , I, O](
    observable: BaseObservable[Self, I],
    caseList: List[PartialFunction[Any, Any]],
    handlerMap: Map[Int, Function[Any, O]]
  ): MatchSplitObservable[Self, I, O] = throw new UnsupportedOperationException("splitMatch without toSignal/toStream is illegal")

}
