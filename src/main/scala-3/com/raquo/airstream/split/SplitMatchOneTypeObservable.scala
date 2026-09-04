package com.raquo.airstream.split

import com.raquo.airstream.core.{Observable, BaseObservable}
import scala.annotation.compileTimeOnly
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, MatchTypeHandler}

/** `MatchTypeObservable` served as macro's data holder for macro expansion.
  *
  * For example:
  *
  * {{{
  * fooSignal.splitMatchOne
  *   .handleType[Baz] { (baz, bazSignal) => renderBazNode(baz, bazSignal) }
  * }}}
  *
  * will be expanded sematically into:
  *
  * {{{
  * MatchTypeObservable.build[*, *, *, Baz](fooSignal)()(???)({ case t: Baz => t })
  * }}}
  *
  * and then into:
  *
  * {{{
  * MatchSplitObservable.build(fooSignal)({ case baz: Baz => baz })(???)
  * }}}
  */

final case class SplitMatchOneTypeObservable[Self[+_] <: Observable[?], I, O, T] private (private val underlying: Unit) extends AnyVal

object SplitMatchOneTypeObservable {

  @compileTimeOnly("`splitMatchOne` without `toSignal`/`toStream` is illegal")
  def build[Self[+_] <: Observable[?], I, O, T](
    observable: BaseObservable[Self, I]
  )(
    caseList: CaseAny*
  )(
    handleList: HandlerAny[O]*
  )(
    tHandler: MatchTypeHandler[T]
  ): SplitMatchOneTypeObservable[Self, I, O, T] =
    throw new UnsupportedOperationException(
      "`splitMatchOne` without `toSignal`/`toStream` is illegal"
    )

}
