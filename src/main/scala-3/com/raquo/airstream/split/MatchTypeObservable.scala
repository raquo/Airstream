package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly

/** `MatchTypeObservable` served as macro's data holder for macro expansion.
 *
 * For example:
 *
 * ```scala
 * fooSignal.splitMatch
 *   .splitType[Baz] { (baz, bazSignal) => renderBazNode(baz, bazSignal) }
 * ```
 *
 * will be expanded sematically into:
 *
 * ```scala
 * MatchTypeObservable.build[*, *, *, Baz](
 *   fooSignal,
 *   Nil,
 *   handlerMap,
 *   ({ case t: Baz => t })
 * )
 * ```
 *
 * and then into:
 *
 * ```scala
 * MatchSplitObservable.build(
 *   fooSignal,
 *   ({ case baz: Baz => baz }) :: Nil,
 *   handlerMap
 * )
 * ```
 */

opaque type MatchTypeObservable[Self[+_] <: Observable[_], I, O, T] = Unit

object MatchTypeObservable {

  @compileTimeOnly("splitMatch without toSignal/toStream is illegal")
  def build[Self[+_] <: Observable[_], I, O, T](
      observable: BaseObservable[Self, I],
      caseList: List[PartialFunction[Any, Any]],
      handlerMap: Map[Int, Function2[Any, Any, O]],
      tCast: PartialFunction[T, T]
  ): MatchTypeObservable[Self, I, O, T] =
    throw new UnsupportedOperationException(
      "splitMatch without toSignal/toStream is illegal"
    )

}
