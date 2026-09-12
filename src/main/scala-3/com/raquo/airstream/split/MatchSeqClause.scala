package com.raquo.airstream.split

import com.raquo.airstream.core.Signal

import scala.annotation.compileTimeOnly

/** A single clause of a `splitMatchSeq` expression, produced by the `handle*` methods of
  * [[SplitMatchSeqContext]]. `O` is the clause handler's result type; the `splitMatchSeq` result
  * type is the least upper bound of all clauses' `O`.
  */
sealed trait MatchSeqClause[+O]

/** The context object passed to each `splitMatchSeq` clause. Write clauses as
  * `_.handleCase { ... } { ... }`, `_.handleType[T] { ... }`, `_.handleValue(v) { ... }`,
  * `_.handleRest { ... }`.
  *
  * `I` is the element type of the collection being split. These methods are never evaluated – the
  * `splitMatchSeq` macro reads the clause shapes at compile time and generates a single match block.
  * Handlers receive a plain `Signal[_]` (not a `StrictSignal[_]`), matching `splitMatchSeq`'s semantics.
  */
sealed trait SplitMatchSeqContext[I] {

  /** Equivalent of one or more `case` branches. Usage:
    * {{{
    * _.handleCase { case Bar(Some(str)) => str } { strSignal => returnValue }`
    * }}}
    */
  @compileTimeOnly("`handleCase` is only usable inside `splitMatchSeq`.")
  def handleCase[A, B, O](pf: PartialFunction[A, B])(handler: Signal[B] => O): MatchSeqClause[O] =
    SplitMatchSeqContext.stub

  /** Equivalent of `case t: T => t`. Usage:
    * {{{
    * _.handleType[T] { tSignal => returnValue }
    * }}}
    */
  @compileTimeOnly("`handleType` is only usable inside `splitMatchSeq`.")
  def handleType[T]: SplitMatchSeqContext.HandleType[I, T] =
    SplitMatchSeqContext.stub

  /** Equivalent of `case `v` => ...` for a singleton `v`. Usage:
    * {{{
    * _.handleValue(SomeObject) { returnValue }
    * }}}
    */
  @compileTimeOnly("`handleValue` is only usable inside `splitMatchSeq`.")
  def handleValue[V, O](v: V)(handler: => O): MatchSeqClause[O] =
    SplitMatchSeqContext.stub

  /** Equivalent of the catch-all `case _ => ...`. It should come last. Usage:
    * {{{
    * _.handleRest { elementSignal => returnValue }
    * }}}
    */
  @compileTimeOnly("`handleRest` is only usable inside `splitMatchSeq`.")
  def handleRest[O](handler: Signal[I] => O): MatchSeqClause[O] =
    SplitMatchSeqContext.stub
}

object SplitMatchSeqContext {

  private def stub: Nothing = throw new UnsupportedOperationException(
    "SplitMatchSeqContext methods are only usable inside `splitMatchSeq`."
  )

  final class HandleType[I, T] private[split] () {
    @compileTimeOnly("`handleType` is only usable inside `splitMatchSeq`.")
    def apply[O](handler: Signal[T] => O): MatchSeqClause[O] = stub
  }
}
