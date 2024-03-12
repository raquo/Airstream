package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal, Observable, BaseObservable}
import scala.quoted.{Expr, Quotes, Type}
import scala.annotation.compileTimeOnly

/**
 * `SplittableTypeMacros` turns this code
 * 
 * ```scala
 * sealed trait Foo
 * final case class Bar(strOpt: Option[String]) extends Foo
 * enum Baz extends Foo {
 *  case Baz1, Baz2
 * }
 * case object Tar extends Foo
 * val splitter = fooSignal.splitMatch
 *  .handleCase { case Bar(Some(str)) => str } { (str, strSignal) => renderStrNode(str, strSignal) }
 *  .handleCase { case baz: Baz => baz } { (baz, bazSignal) => renderBazNode(baz, bazSignal) }
 *  .handleCase {
 *    case Tar => ()
 *    case _: Int => ()
 *  } { (_, _) => div("Taz") }
 *  .toSignal
 * ```
 * 
 * into this code:
 *
 * ```scala
 * val splitter = fooSignal.
 *  .map { i =>
 *    i match {
 *      case Bar(Some(str)) => (0, str)
 *      case baz: Baz => (1, baz)
 *      case Tar => (2, ())
 *      case _: Int => (2, ())
 *    }
 *  }
 *  .splitOne(_._1) { ??? }
 * ```
 * 
 * After macros expansion, compiler will warns above code "match may not be exhaustive" and "unreachable case" as expected.
 */
object SplittableTypeMacros {

  /**
   * `MatchSplitObservable` served as macro's data holder for macro expansion.
   * 
   * Like any class runtime builder, which uses methods to add various data and a dedicated method to build an instance of the class, `MatchSplitObservable` uses `handleCase`s to keep all code data and `toSignal`/`toStream` to expands macro into to match case definition.
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
   *
   * This is important, because `({ case baz: Baz => baz }) :: ({ case Bar(Some(str)) => str }) :: Nil` saves all the infomation needed for the macro to expands into match case definition
   */
  final class MatchSplitObservable[Self[+_] <: Observable[_] , I, O] {
    throw new UnsupportedOperationException("splitMatch without toSignal/toStream is illegal")
  }

  object MatchSplitObservable {
    @compileTimeOnly("splitMatch without toSignal/toStream is illegal")
    def build[Self[+_] <: Observable[_] , I, O](
      observable: BaseObservable[Self, I],
      caseList: List[PartialFunction[Any, Any]],
      handlerMap: Map[Int, Function[Any, O]]
    ): MatchSplitObservable[Self, I, O] = throw new UnsupportedOperationException("splitMatch without toSignal/toStream is illegal")
  }

  extension [Self[+_] <: Observable[_], I](inline observable: BaseObservable[Self, I]) {
    inline def splitMatch: MatchSplitObservable[Self, I, Nothing] = MatchSplitObservable.build(observable, Nil, Map.empty[Int, Function[Any, Nothing]])
  }

  extension [Self[+_] <: Observable[_], I, O](inline matchSplitObservable: MatchSplitObservable[Self, I, O]) {
    inline def handleCase[A, B, O1 >: O](inline casePf: PartialFunction[A, B])(inline handleFn: ((B, Signal[B])) => O1) = ${ handleCaseImpl('{ matchSplitObservable }, '{ casePf }, '{ handleFn })}
  }

  extension [I, O](inline matchSplitObservable: MatchSplitObservable[Signal, I, O]) {
    inline def toSignal: Signal[O] = ${ observableImpl('{ matchSplitObservable })}
  }

  extension [I, O](inline matchSplitObservable: MatchSplitObservable[EventStream, I, O]) {
    inline def toStream: EventStream[O] = ${ observableImpl('{ matchSplitObservable })}
  }

  private def handleCaseImpl[Self[+_] <: Observable[_] : Type, I: Type, O: Type, O1 >: O : Type, A: Type, B: Type](
    matchSplitObservableExpr: Expr[MatchSplitObservable[Self, I, O]],
    casePfExpr: Expr[PartialFunction[A, B]],
    handleFnExpr: Expr[Function[(B, Signal[B]), O1]],
  )(
    using quotes: Quotes
  ): Expr[MatchSplitObservable[Self, I, O1]] = {
    import quotes.reflect.*
    
    matchSplitObservableExpr match {
      case '{ MatchSplitObservable.build[Self, I, O]($observableExpr, $caseListExpr, $handlerMapExpr)} => innerHandleCaseImpl(observableExpr, caseListExpr, handlerMapExpr, casePfExpr, handleFnExpr)
      case other => report.errorAndAbort("Macro expansion failed, please use `splitMatch` instead of creating new MatchSplitObservable explicitly")
    }
  }

  private def innerHandleCaseImpl[Self[+_] <: Observable[_] : Type, I: Type, O: Type, O1 >: O : Type, A: Type, B: Type](
    observableExpr: Expr[BaseObservable[Self, I]],
    caseListExpr: Expr[List[PartialFunction[Any, Any]]],
    handlerMapExpr: Expr[Map[Int, Function[Any, O]]],
    casePfExpr: Expr[PartialFunction[A, B]],
    handleFnExpr: Expr[Function[(B, Signal[B]), O1]]
  )(
    using quotes: Quotes
  ): Expr[MatchSplitObservable[Self, I, O1]] = {
    val caseExprList = exprOfListToListOfExpr(caseListExpr)

    val nextCaseExprList = casePfExpr.asExprOf[PartialFunction[Any, Any]] :: caseExprList

    val nextCaseListExpr = listOfExprToExprOfList(nextCaseExprList)

    '{ MatchSplitObservable.build[Self, I, O1]($observableExpr, $nextCaseListExpr, ($handlerMapExpr + ($handlerMapExpr.size -> $handleFnExpr.asInstanceOf[Function[Any, O1]]))) }
  }

  private def exprOfListToListOfExpr(
    pfListExpr: Expr[List[PartialFunction[Any, Any]]]
  )(
    using quotes: Quotes
  ): List[Expr[PartialFunction[Any, Any]]] = {
    import quotes.reflect.*
    
    pfListExpr match {
      case '{ $headExpr :: (${tailExpr}: List[PartialFunction[Any, Any]]) } =>
        headExpr :: exprOfListToListOfExpr(tailExpr)
      case '{ Nil } => Nil
      case _ => report.errorAndAbort("Macro expansion failed, please use `handleCase` instead of modify MatchSplitObservable explicitly")
    }
    
  }

  private def listOfExprToExprOfList(
    pfExprList: List[Expr[PartialFunction[Any, Any]]]
  )(
    using quotes: Quotes
  ): Expr[List[PartialFunction[Any, Any]]] = {
    import quotes.reflect.*
    
    pfExprList match
      case head :: tail => '{ $head :: ${listOfExprToExprOfList(tail)} }
      case Nil => '{ Nil }
  }

  private inline def toSplittableOneObservable[Self[+_] <: Observable[_], O](
    parentObservable: BaseObservable[Self, (Int, Any)],
    handlerMap: Map[Int, Function[Any, O]]
  ): Self[O] = {
    parentObservable.matchStreamOrSignal(
      ifStream = _.splitOne(_._1) { case (idx, (_, b), dataSignal) =>
        val bSignal = dataSignal.map(_._2)
        handlerMap.apply(idx).apply(b -> bSignal)
      },
      ifSignal = _.splitOne(_._1) { case (idx, (_, b), dataSignal) =>
        val bSignal = dataSignal.map(_._2)
        handlerMap.apply(idx).apply(b -> bSignal)
      }
    ).asInstanceOf[Self[O]] // #TODO[Integrity] Same as FlatMap/AsyncStatusObservable, how to type this properly?
  }

  private def observableImpl[Self[+_] <: Observable[_] : Type, I: Type, O: Type](
    matchSplitObservableExpr: Expr[MatchSplitObservable[Self, I, O]]
  )(
    using quotes: Quotes
  ): Expr[Self[O]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{ MatchSplitObservable.build[Self, I, O]($_, Nil, $_)} =>
        report.errorAndAbort("Macro expansion failed, need at least one handleCase")
      case '{ MatchSplitObservable.build[Self, I, O]($observableExpr, $caseListExpr, $handlerMapExpr)} => 
        '{toSplittableOneObservable(
          $observableExpr
            .map(i => ${ innerObservableImpl('i, caseListExpr) })
            .asInstanceOf[BaseObservable[Self, (Int, Any)]],
          $handlerMapExpr
        )}
      case _ => report.errorAndAbort("Macro expansion failed, please use `splitMatch` instead of creating new MatchSplitObservable explicitly")
    }
  }

  private def innerObservableImpl[I: Type](
    iExpr: Expr[I],
    caseListExpr: Expr[List[PartialFunction[Any, Any]]]
  )(
    using quotes: Quotes
  ): Expr[(Int, Any)] = {
    import quotes.reflect.*

    val caseExprList = exprOfListToListOfExpr(caseListExpr)

    val allCaseDefLists = caseExprList.reverse.zipWithIndex.flatMap { case (caseExpr, idx) =>
      caseExpr.asTerm match {
        case Lambda(_, Match(_, caseDefList)) => {
          caseDefList.map { caseDef =>
            val idxExpr = Expr.apply(idx)
            val newRhsExpr = '{ val res = ${caseDef.rhs.asExprOf[Any]}; ($idxExpr, res)}
            CaseDef.copy(caseDef)(caseDef.pattern, caseDef.guard, newRhsExpr.asTerm)
          }
        }
        case _ => report.errorAndAbort("Macro expansion failed, please use `handleCase` with annonymous partial function")
      }
    }.map(_.changeOwner(Symbol.spliceOwner))

    Match(iExpr.asTerm, allCaseDefLists).asExprOf[(Int, Any)]
  }

}
