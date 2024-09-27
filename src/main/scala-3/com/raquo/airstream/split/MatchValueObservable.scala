package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly

/** `MatchSingletonObservable` served as macro's data holder for macro expansion.
 *
 * For example:
 *
 * ```scala
 * fooSignal.splitMatch
 *   .splitValue(Tar)(tarSignal => renderTarNode(tarSignal))
 * ```
 * 
 * will be expanded sematically into:
 *
 * ```scala
 * MatchTypeObservable.build[*, *, *, Baz](
 *   fooSignal,
 *   Nil,
 *   handlerMap,
 *   ({ case Tar => Tar })
 * )
 * ```
 *
 * and then into:
 *
 * ```scala
 * MatchSplitObservable.build(
 *   fooSignal,
 *   ({ case Tar => Tar }) :: Nil,
 *   handlerMap
 * )
 * ```
 */

opaque type MatchValueObservable[Self[+_] <: Observable[_], I, O, V0, V1] = Unit

object MatchValueObservable {

  @compileTimeOnly("splitMatch without toSignal/toStream is illegal")
  def build[Self[+_] <: Observable[_], I, O, V0, V1](
      observable: BaseObservable[Self, I],
      caseList: List[PartialFunction[Any, Any]],
      handlerMap: Map[Int, Function[Any, O]],
      vCast: PartialFunction[V0, V1]
  ): MatchValueObservable[Self, I, O, V0, V1] =
    throw new UnsupportedOperationException(
      "splitMatch without toSignal/toStream is illegal"
    )

}
