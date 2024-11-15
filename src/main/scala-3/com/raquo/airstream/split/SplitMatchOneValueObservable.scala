package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, MatchValueHandler}

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
 * MatchTypeObservable.build[*, *, *, Baz](fooSignal)()(???)({ case Tar => Tar })
 * ```
 *
 * and then into:
 *
 * ```scala
 * MatchSplitObservable.build(fooSignal)({ case Tar => Tar })(???)
 * ```
 */

final case class SplitMatchOneValueObservable[Self[+_] <: Observable[_], I, O, V] private (private val underlying: Unit) extends AnyVal

object SplitMatchOneValueObservable {

  @compileTimeOnly("`splitMatchOne` without `toSignal`/`toStream` is illegal")
  def build[Self[+_] <: Observable[_], I, O, V](
    observable: BaseObservable[Self, I]
  )(
    caseList: CaseAny*
  )(
    handleList: HandlerAny[O]*
  )(
    vHandler: MatchValueHandler[V]
  ): SplitMatchOneValueObservable[Self, I, O, V] =
    throw new UnsupportedOperationException(
      "`splitMatchOne` without `toSignal`/`toStream` is illegal"
    )

}
