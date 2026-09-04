package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny}

/**
 * `MatchSplitObservable` served as macro's data holder for macro expansion.
 *
 * For example:
 *
 * ```scala
 * fooSignal.splitMatchOne
 *  .handleCase { case Bar(Some(str)) => str } { (str, strSignal) => renderStrNode(str, strSignal) }
 *    .handleCase { case baz: Baz => baz } { (baz, bazSignal) => renderBazNode(baz, bazSignal) }
 * ```
 *
 * will be expanded sematically into:
 *
 * ```scala
 *  MatchSplitObservable.build(fooSignal)(({ case baz: Baz => baz }), ({ case Bar(Some(str)) => str }))(...)
 * ```
 */

final case class SplitMatchOneObservable[Self[+_] <: Observable[?] , I, O] private (private val underlying: Unit) extends AnyVal

object SplitMatchOneObservable {

  @compileTimeOnly("`splitMatchOne` without `toSignal`/`toStream` is illegal")
  def build[Self[+_] <: Observable[?] , I, O](
    observable: BaseObservable[Self, I]
  )(
    caseList: CaseAny*
  )(
    handleList: HandlerAny[O]*
  ): SplitMatchOneObservable[Self, I, O] = throw new UnsupportedOperationException("`splitMatchOne` without `toSignal`/`toStream` is illegal")

}
