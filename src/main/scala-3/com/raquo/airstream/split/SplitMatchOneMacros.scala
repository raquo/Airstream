package com.raquo.airstream.split

import com.raquo.airstream.core.{EventStream, Signal, Observable, BaseObservable}
import scala.quoted.{Expr, Quotes, Type}
import scala.annotation.{unused, targetName}
import scala.compiletime.summonInline
import scala.quoted.Varargs
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, MatchTypeHandler, MatchValueHandler, innerObservableImpl}
import scala.reflect.TypeTest

/** `SplitMatchOneMacros` turns this code:
  *
  * {{{
  * sealed trait Foo
  * final case class Bar(strOpt: Option[String]) extends Foo
  * enum Baz extends Foo {
  *   case Baz1, Baz2
  * }
  * case object Tar extends Foo
  * val splitter = fooSignal
  *   .splitMatchOne
  *   .handleCase { case Bar(Some(str)) => str } { (str, strSignal) =>
  *     renderStrNode(str, strSignal)
  *   }
  *   .handleCase { case baz: Baz => baz } { (baz, bazSignal) =>
  *     renderBazNode(baz, bazSignal)
  *   }
  *   .handleCase {
  *     case Tar    => ()
  *     case _: Int => ()
  *   } { (_, _) => div("Taz") }
  *   .toSignal
  * }}}
  *
  * into this code:
  *
  * {{{
  * val splitter = fooSignal
  *  .map { i =>
  *    i match {
  *      case Bar(Some(str)) => (0, str)
  *      case baz: Baz => (1, baz)
  *      case Tar => (2, ())
  *      case _: Int => (2, ())
  *    }
  *  }
  *  .splitOne(_._1) { ??? }
  * }}}
  *
  * After macros expansion, compiler will warns above code "match may not be
  * exhaustive" and "unreachable case" as expected.
  */
object SplitMatchOneMacros {

  private[airstream] inline def delegateHandleCase[Self[+_] <: Observable[_], I, O, A, B, O1 >: O](
    inline matchSplitObservable: SplitMatchOneObservable[Self, I, O],
    inline casePf: PartialFunction[A, B],
    inline handleFn: (B, Signal[B]) => O1
  ) = ${
    handleCaseImpl('{ matchSplitObservable }, '{ casePf }, '{ handleFn })
  }

  private[airstream] inline def delegateHandleType[Self[+_] <: Observable[_], I, O, T](
    inline matchSplitObservable: SplitMatchOneObservable[Self, I, O]
  ) = ${
    handleTypeImpl[Self, I, O, T]('{ matchSplitObservable })
  }

  private[airstream] inline def delegateHandleValue[Self[+_] <: Observable[_], I, O, V](
    inline matchSplitObservable: SplitMatchOneObservable[Self, I, O],
    inline v: V
  )(
    using inline valueOf: ValueOf[V]
  ) = ${
    handleValueImpl[Self, I, O, V]('{ matchSplitObservable }, '{ v })
  }

  private[airstream] inline def delegateHandleTypeApply[Self[+_] <: Observable[_], I, O, T, O1 >: O](
    inline matchTypeObserver: SplitMatchOneTypeObservable[Self, I, O, T],
    inline handleFn: (T, Signal[T]) => O1
  ) = ${
    handleTypeApplyImpl('{ matchTypeObserver }, '{ handleFn })
  }

  private[airstream] inline def delegateHandleValueApply[Self[+_] <: Observable[_], I, O, V, O1 >: O](
    inline matchValueObservable: SplitMatchOneValueObservable[Self, I, O, V],
    inline handleFn: (V, Signal[V]) => O1
  ) = ${
    handleValueApplyImpl('{ matchValueObservable }, '{ handleFn })
  }

  private[airstream] inline def delegateToSignal[I, O](
    inline matchSplitObservable: SplitMatchOneObservable[Signal, I, O]
  ) = ${
    observableImpl('{ matchSplitObservable })
  }

  private[airstream] inline def delegateToStream[I, O](
    inline matchSplitObservable: SplitMatchOneObservable[EventStream, I, O]
  ) = ${
    observableImpl('{ matchSplitObservable })
  }

  private def handleCaseImpl[Self[+_] <: Observable[_]: Type, I: Type, O: Type, O1 >: O: Type, A: Type, B: Type](
    matchSplitObservableExpr: Expr[SplitMatchOneObservable[Self, I, O]],
    casePfExpr: Expr[PartialFunction[A, B]],
    handleFnExpr: Expr[Function2[B, Signal[B], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchOneObservable[Self, I, O1]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchOneObservable.build[Self, I, O]($observableExpr)(${caseExpr}*)(${handlerExpr}*)
          } => {
            val caseExprSeq = caseExpr match {
              case Varargs(caseExprSeq) => caseExprSeq
              case _ => report.errorAndAbort(
                "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
              )
            }

            val handlerExprSeq = handlerExpr match {
              case Varargs(handlerExprSeq) => handlerExprSeq
              case _ => report.errorAndAbort(
                "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
              )
            }

            innerHandleCaseImpl(
              observableExpr,
              caseExprSeq,
              handlerExprSeq,
              casePfExpr,
              handleFnExpr
            )
          }
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
        )
    }
  }

  private def handleTypeImpl[Self[+_] <: Observable[_]: Type, I: Type, O: Type, T: Type](
    matchSplitObservableExpr: Expr[SplitMatchOneObservable[Self, I, O]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchOneTypeObservable[Self, I, O, T]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchOneObservable.build[Self, I, O]($observableExpr)(${caseExpr}*)(${handlerExpr}*)
          } =>
        '{
          SplitMatchOneTypeObservable.build[Self, I, O, T](
            $observableExpr
          )(
            ${caseExpr}*
          )(
            ${handlerExpr}*
          )(
            MatchTypeHandler.instance[T]
          )
        }
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
        )
    }
  }

  private def handleTypeApplyImpl[Self[+_] <: Observable[_]: Type, I: Type, O: Type, O1 >: O: Type, T: Type](
    matchSplitObservableExpr: Expr[SplitMatchOneTypeObservable[Self, I, O, T]],
    handleFnExpr: Expr[Function2[T, Signal[T], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchOneObservable[Self, I, O1]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchOneTypeObservable.build[Self, I, O, T]($observableExpr)(${caseExpr}*)(${handlerExpr}*)(MatchTypeHandler.instance[T])
          } =>
        val caseExprSeq = caseExpr match {
          case Varargs(caseExprSeq) => caseExprSeq
          case _ => report.errorAndAbort(
            "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
          )
        }

        val handlerExprSeq = handlerExpr match {
          case Varargs(handlerExprSeq) => handlerExprSeq
          case _ => report.errorAndAbort(
            "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
          )
        }

        val tCaseExpr: Expr[PartialFunction[T, T]] = '{ { case t: T => t } }

        innerHandleCaseImpl[Self, I, O, O1, T, T](
          observableExpr,
          caseExprSeq,
          handlerExprSeq,
          tCaseExpr,
          handleFnExpr
        )
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
        )
    }
  }

  private def handleValueImpl[Self[+_] <: Observable[_]: Type, I: Type, O: Type, V: Type](
    matchSplitObservableExpr: Expr[SplitMatchOneObservable[Self, I, O]],
    vExpr: Expr[V]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchOneValueObservable[Self, I, O, V]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchOneObservable.build[Self, I, O]($observableExpr)(${caseExpr}*)(${handlerExpr}*)
          } =>
        '{
          SplitMatchOneValueObservable.build[Self, I, O, V](
            $observableExpr
          )(
            ${caseExpr}*
          )(
            ${handlerExpr}*
          )(
            MatchValueHandler.instance[V]($vExpr)
          )
        }
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
        )
    }
  }

  private def handleValueApplyImpl[Self[+_] <: Observable[_]: Type, I: Type, O: Type, O1 >: O: Type, V: Type](
    matchValueObservableExpr: Expr[SplitMatchOneValueObservable[Self, I, O, V]],
    handleFnExpr: Expr[Function2[V, Signal[V], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchOneObservable[Self, I, O1]] = {
    import quotes.reflect.*

    matchValueObservableExpr match {
      case '{
            SplitMatchOneValueObservable.build[Self, I, O, V]($observableExpr)(${caseExpr}*)(${handlerExpr}*)(MatchValueHandler.instance[V]($vExpr))
          } =>
        val caseExprSeq = caseExpr match {
          case Varargs(caseExprSeq) => caseExprSeq
          case _ => report.errorAndAbort(
            "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
          )
        }

        val handlerExprSeq = handlerExpr match {
          case Varargs(handlerExprSeq) => handlerExprSeq
          case _ => report.errorAndAbort(
            "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
          )
        }

        val vCaseExpr: Expr[PartialFunction[V, V]] = '{ { case _: V => $vExpr } }

        innerHandleCaseImpl[Self, I, O, O1, V, V](
          observableExpr,
          caseExprSeq,
          handlerExprSeq,
          vCaseExpr,
          handleFnExpr
        )
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
        )
    }
  }

  private def innerHandleCaseImpl[Self[+_] <: Observable[_]: Type, I: Type, O: Type, O1 >: O: Type, A: Type, B: Type](
    observableExpr: Expr[BaseObservable[Self, I]],
    caseExprSeq: Seq[Expr[CaseAny]],
    handlerExprSeq: Seq[Expr[HandlerAny[O]]],
    casePfExpr: Expr[PartialFunction[A, B]],
    handleFnExpr: Expr[Function2[B, Signal[B], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchOneObservable[Self, I, O1]] = {

    '{
      SplitMatchOneObservable.build[Self, I, O1](
        $observableExpr
      )(
        ${ Varargs(caseExprSeq :+ casePfExpr.asExprOf[CaseAny]) }*
      )(
        ${ Varargs(handlerExprSeq :+ handleFnExpr.asExprOf[HandlerAny[O1]]) }*
      )
    }
  }

  private def toSplittableOneObservable[Self[+_] <: Observable[_], O](
    parentObservable: BaseObservable[Self, (Int, Any)],
    handlers: HandlerAny[O]*
  ): Self[O] = {
    parentObservable
      .matchStreamOrSignal(
        ifStream = _.splitOne(_._1) { case (idx, (_, b), dataSignal) =>
          val bSignal = dataSignal.map(_._2)
          handlers.view.zipWithIndex.map(_.swap).toMap
            .getOrElse(idx, IllegalStateException("Illegal SplitMatchOne state. This is a bug in Airstream."))
            .asInstanceOf[Function2[Any, Any, O]]
            .apply(b, bSignal)
        },
        ifSignal = _.splitOne(_._1) { case (idx, (_, b), dataSignal) =>
          val bSignal = dataSignal.map(_._2)
          handlers.view.zipWithIndex.map(_.swap).toMap
            .getOrElse(idx, IllegalStateException("Illegal SplitMatchOne state. This is a bug in Airstream."))
            .asInstanceOf[Function2[Any, Any, O]]
            .apply(b, bSignal)
        }
      )
      .asInstanceOf[Self[O]] // #TODO[Integrity] Same as FlatMap/AsyncStatusObservable, how to type this properly?
  }

  private def observableImpl[Self[+_] <: Observable[_]: Type, I: Type, O: Type](
    matchSplitObservableExpr: Expr[SplitMatchOneObservable[Self, I, O]]
  )(
    using quotes: Quotes
  ): Expr[Self[O]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchOneObservable.build[Self, I, O]($observableExpr)(${caseExpr}*)(${handleExpr}*)
          } =>
        val caseExprSeq = caseExpr match {
          case Varargs(caseExprSeq) => caseExprSeq
          case _ => report.errorAndAbort(
            "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
          )
        }
        if (caseExprSeq.isEmpty) {
          report.errorAndAbort(
            "Macro expansion failed, need at least one handleCase"
          )
        } else {
          '{
            toSplittableOneObservable(
              $observableExpr
                .map(i => ${ innerObservableImpl('i, caseExprSeq) })
                .asInstanceOf[BaseObservable[Self, (Int, Any)]],
              ${handleExpr}*
            )
          }
        }
      case _ =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchOne` instead of creating new SplitMatchOneObservable explicitly"
        )
    }
  }

}
