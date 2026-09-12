package com.raquo.airstream.split

import com.raquo.airstream.state.StrictSignal

import scala.annotation.compileTimeOnly

/** A single clause of a `splitMatchOne` expression, produced by the `handle*` methods of
  * [[SplitMatchOneContext]]. `O` is the clause handler's result type; the `splitMatchOne` result
  * type is the least upper bound of all clauses' `O`.
  */
sealed trait MatchOneClause[+O]

/** The context object passed to each `splitMatchOne` clause. Write clauses as
  * `_.handleCase { ... } { ... }`, `_.handleType[T] { ... }`, `_.handleValue(v) { ... }`,
  * `_.handleRest { ... }`.
  *
  * `I` is the input type of the observable being split. These methods are never evaluated – the
  * `splitMatchOne` macro reads the clause shapes at compile time and generates a single match block.
  */
sealed trait SplitMatchOneContext[I] {

  /** Equivalent of one or more `case` branches. Usage:
    * {{{
    * _.handleCase { case Bar(Some(str)) => str } { strSignal => returnValue }`
    * }}}
    */
  @compileTimeOnly("`handleCase` is only usable inside `splitMatchOne`.")
  def handleCase[A, B, O](pf: PartialFunction[A, B])(handler: StrictSignal[B] => O): MatchOneClause[O] =
    SplitMatchOneContext.stub

  /** Equivalent of `case t: T => t`. Usage:
    * {{{
    * _.handleType[T] { tSignal => returnValue }
    * }}}
    */
  @compileTimeOnly("`handleType` is only usable inside `splitMatchOne`.")
  def handleType[T]: SplitMatchOneContext.HandleType[I, T] =
    SplitMatchOneContext.stub

  /** Equivalent of `case `v` => ...` for a singleton `v`. Usage:
    * {{{
    * _.handleValue(SomeObject) { returnValue }
    * }}}
    */
  @compileTimeOnly("`handleValue` is only usable inside `splitMatchOne`.")
  def handleValue[V, O](v: V)(handler: => O): MatchOneClause[O] =
    SplitMatchOneContext.stub

  /** Equivalent of the catch-all `case _ => ...`. It should come last. Usage:
    * {{{
    * _.handleRest { parentValueSignal => returnValue }
    * }}}
    */
  @compileTimeOnly("`handleRest` is only usable inside `splitMatchOne`.")
  def handleRest[O](handler: StrictSignal[I] => O): MatchOneClause[O] =
    SplitMatchOneContext.stub
}

object SplitMatchOneContext {

  private def stub: Nothing = throw new UnsupportedOperationException(
    "SplitMatchOneContext methods are only usable inside `splitMatchOne`."
  )

  final class HandleType[I, T] private[split] () {
    @compileTimeOnly("`handleType` is only usable inside `splitMatchOne`.")
    def apply[O](handler: StrictSignal[T] => O): MatchOneClause[O] = stub
  }
}
