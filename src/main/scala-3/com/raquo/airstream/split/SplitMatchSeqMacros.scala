package com.raquo.airstream.split

import com.raquo.airstream.core.{
  EventStream,
  Signal,
  Observable,
  BaseObservable
}
import scala.quoted.{Expr, Quotes, Type}
import scala.annotation.{unused, targetName}
import scala.compiletime.summonInline
import scala.quoted.Varargs
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, MatchTypeHandler, MatchValueHandler, innerObservableImpl}

object SplitMatchSeqMacros {

  private[airstream] inline def delegateHandleCase[Self[+_] <: Observable[_], I, K, O, CC[_], A, B, O1 >: O](
    inline matchSplitObservable: SplitMatchSeqObservable[Self, I, K, O, CC],
    inline casePf: PartialFunction[A, B],
    inline handleFn: (B, Signal[B]) => O1
  ) = ${
    handleCaseImpl('{ matchSplitObservable }, '{ casePf }, '{ handleFn })
  }

  private[airstream] inline def delegateHandleType[Self[+_] <: Observable[_], I, K, O, CC[_], T](
    inline matchSplitObservable: SplitMatchSeqObservable[Self, I, K, O, CC]
  ) = ${
    handleTypeImpl[Self, I, K, O, CC, T]('{ matchSplitObservable })
  }

  private[airstream] inline def delegateHandleValue[Self[+_] <: Observable[_], I, K, O, CC[_], V](
    inline matchSplitObservable: SplitMatchSeqObservable[Self, I, K, O, CC],
    inline v: V
  )(
    using inline valueOf: ValueOf[V]
  ) = ${
    handleValueImpl('{ matchSplitObservable }, '{ v })
  }

  private[airstream] inline def delegateHandleTypeApply[Self[+_] <: Observable[_], I, K, O, CC[_], T, O1 >: O](
    inline matchTypeObserver: SplitMatchSeqTypeObservable[Self, I, K, O, CC, T],
    inline handleFn: (T, Signal[T]) => O1
  ) = ${
    handleTypeApplyImpl('{ matchTypeObserver }, '{ handleFn })
  }

  private[airstream] inline def delegateHandleValueApply[Self[+_] <: Observable[_], I, K, O, CC[_], V, O1 >: O](
    inline matchValueObservable: SplitMatchSeqValueObservable[Self, I, K, O, CC, V],
    inline handleFn: (V, Signal[V]) => O1
  ) = ${
    handleValueApplyImpl('{ matchValueObservable }, '{ handleFn })
  }

  private[airstream] inline def delegateToSignal[Self[+_] <: Observable[_], I, K, O, CC[_]](
    inline matchSplitObservable: SplitMatchSeqObservable[Self, I, K, O, CC]
  ) = ${ observableImpl('{ matchSplitObservable }) }

  private def handleCaseImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, O1 >: O: Type, CC[_]: Type, A: Type, B: Type](
    matchSplitObservableExpr: Expr[SplitMatchSeqObservable[Self, I, K, O, CC]],
    casePfExpr: Expr[PartialFunction[A, B]],
    handleFnExpr: Expr[Function2[B, Signal[B], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchSeqObservable[Self, I, K, O1, CC]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchSeqObservable.build[Self, I, K, O, CC](
              $keyFnExpr,
              $distinctComposeExpr,
              $duplicateKeysConfigExpr,
              $observableExpr
            )(
              ${caseExpr}*
            )(
              ${handlerExpr}*
            )
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
              keyFnExpr,
              distinctComposeExpr,
              duplicateKeysConfigExpr,
              observableExpr,
              caseExprSeq,
              handlerExprSeq,
              casePfExpr,
              handleFnExpr
            )
          }
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

  private def handleTypeImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, CC[_]: Type, T: Type](
    matchSplitObservableExpr: Expr[SplitMatchSeqObservable[Self, I, K, O, CC]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchSeqTypeObservable[Self, I, K, O, CC, T]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchSeqObservable.build[Self, I, K, O, CC](
              $keyFnExpr,
              $distinctComposeExpr,
              $duplicateKeysConfigExpr,
              $observableExpr
            )(
              ${caseExpr}*
            )(
              ${handlerExpr}*
            )
          } =>
        '{
          SplitMatchSeqTypeObservable.build[Self, I, K, O, CC, T](
            $keyFnExpr,
            $distinctComposeExpr,
            $duplicateKeysConfigExpr,
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
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

  private def handleTypeApplyImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, O1 >: O: Type, CC[_]: Type, T: Type](
    matchSplitObservableExpr: Expr[SplitMatchSeqTypeObservable[Self, I, K, O, CC, T]],
    handleFnExpr: Expr[Function2[T, Signal[T], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchSeqObservable[Self, I, K, O1, CC]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchSeqTypeObservable.build[Self, I, K, O, CC, T](
              $keyFnExpr,
              $distinctComposeExpr,
              $duplicateKeysConfigExpr,
              $observableExpr
            )(
              ${caseExpr}*
            )(
              ${handlerExpr}*
            )(
              MatchTypeHandler.instance[T]
            )
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

            val tCaseExpr: Expr[PartialFunction[T, T]] = '{ { case t: T => t } }

            innerHandleCaseImpl(
              keyFnExpr,
              distinctComposeExpr,
              duplicateKeysConfigExpr,
              observableExpr,
              caseExprSeq,
              handlerExprSeq,
              tCaseExpr,
              handleFnExpr
            )
          }
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

  private def handleValueImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, CC[_]: Type, V: Type](
    matchSplitObservableExpr: Expr[SplitMatchSeqObservable[Self, I, K, O, CC]],
    vExpr: Expr[V]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchSeqValueObservable[Self, I, K, O, CC, V]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchSeqObservable.build[Self, I, K, O, CC](
              $keyFnExpr,
              $distinctComposeExpr,
              $duplicateKeysConfigExpr,
              $observableExpr
            )(
              ${caseExpr}*
            )(
              ${handlerExpr}*
            )
          } =>
        '{
          SplitMatchSeqValueObservable.build[Self, I, K, O, CC, V](
            $keyFnExpr,
            $distinctComposeExpr,
            $duplicateKeysConfigExpr,
            $observableExpr
          )(
            ${caseExpr}*
          )(
            ${handlerExpr}*
          )(
            MatchValueHandler.instance($vExpr)
          )
        }
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

  private def handleValueApplyImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, O1 >: O: Type, CC[_]: Type, V: Type](
    matchValueObservableExpr: Expr[SplitMatchSeqValueObservable[Self, I, K, O, CC, V]],
    handleFnExpr: Expr[Function2[V, Signal[V], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchSeqObservable[Self, I, K, O1, CC]] = {
    import quotes.reflect.*

    matchValueObservableExpr match {
      case '{
            SplitMatchSeqValueObservable.build[Self, I, K, O, CC, V](
              $keyFnExpr,
              $distinctComposeExpr,
              $duplicateKeysConfigExpr,
              $observableExpr
            )(
              ${caseExpr}*
            )(
              ${handlerExpr}*
            )(
              MatchValueHandler.instance($vExpr)
            )
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

            val vCaseExpr: Expr[PartialFunction[V, V]] = '{ { case _: V => $vExpr } }

            innerHandleCaseImpl(
              keyFnExpr,
              distinctComposeExpr,
              duplicateKeysConfigExpr,
              observableExpr,
              caseExprSeq,
              handlerExprSeq,
              vCaseExpr,
              handleFnExpr
            )
          }
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

  private def innerHandleCaseImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, O1 >: O: Type, CC[_]: Type, A: Type, B: Type](
    keyFnExpr: Expr[Function1[I, K]],
    distinctComposeExpr: Expr[Function1[Signal[I], Signal[I]]],
    duplicateKeysConfigExpr: Expr[DuplicateKeysConfig],
    observableExpr: Expr[BaseObservable[Self, CC[I]]],
    caseExprSeq: Seq[Expr[CaseAny]],
    handlerExprSeq: Seq[Expr[HandlerAny[O]]],
    casePfExpr: Expr[PartialFunction[A, B]],
    handleFnExpr: Expr[Function2[B, Signal[B], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchSeqObservable[Self, I, K, O1, CC]] = {

    '{
      SplitMatchSeqObservable.build[Self, I, K, O1, CC](
        $keyFnExpr,
        $distinctComposeExpr,
        $duplicateKeysConfigExpr,
        $observableExpr,
      )(
        ${ Varargs(caseExprSeq :+ casePfExpr.asExprOf[CaseAny]) }*
      )(
        ${ Varargs(handlerExprSeq :+ handleFnExpr.asExprOf[HandlerAny[O1]]) }*
      )
    }
  }

  private inline def customDistinctCompose[I](
    distinctCompose: Signal[I] => Signal[I]
  )(
    dataSignal: Signal[(I, Int, Any)]
  ): Signal[(I, Int, Any)] = {
    val iSignal = dataSignal.map(_._1)
    val otherSignal = dataSignal.map(data => data._2 -> data._3)
    // TODO: We are unnecessary sufferring from `otherSignal.distinct`'s cost here
    distinctCompose(iSignal).combineWith(otherSignal.distinct)
  }

  private inline def customKey[I, K](
    keyFn: I => K
  )(
    input: (I, Int, Any)
  ): (Int, K) = {
    val (i, idx, _) = input
    idx -> keyFn(i)
  }

  private def toSplittableSeqObservable[Self[+_] <: Observable[_], I, K, O, CC[_]](
    parentObservable: BaseObservable[Self, CC[(I, Int, Any)]],
    keyFn: I => K,
    distinctCompose: Signal[I] => Signal[I],
    duplicateKeysConfig: DuplicateKeysConfig,
    splittable: Splittable[CC],
    handlers: HandlerAny[O]*,
  ): Signal[CC[O]] = {
    parentObservable
      .matchStreamOrSignal(
        ifStream = _.split(
          key = customKey(keyFn),
          distinctCompose = customDistinctCompose(distinctCompose),
          duplicateKeys = duplicateKeysConfig
        ) { case ((idx, _), (_, _, b), dataSignal) =>
          val bSignal = dataSignal.map(_._3)
          handlers.view.zipWithIndex.map(_.swap).toMap
            .getOrElse(idx, IllegalStateException("Illegal SplitMatchSeq state. This is a bug in Airstream."))
            .asInstanceOf[Function2[Any, Any, O]]
            .apply(b, bSignal)
        }(splittable),
        ifSignal = _.split(
          key = customKey(keyFn),
          distinctCompose = customDistinctCompose(distinctCompose),
          duplicateKeys = duplicateKeysConfig
        ) { case ((idx, _), (_, _, b), dataSignal) =>
          val bSignal = dataSignal.map(_._3)
          handlers.view.zipWithIndex.map(_.swap).toMap
            .getOrElse(idx, IllegalStateException("Illegal SplitMatchSeq state. This is a bug in Airstream."))
            .asInstanceOf[Function2[Any, Any, O]]
            .apply(b, bSignal)
        }(splittable)
      )
  }

  private def observableImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, CC[_]: Type](
    matchSplitObservableExpr: Expr[SplitMatchSeqObservable[Self, I, K, O, CC]]
  )(
    using quotes: Quotes
  ): Expr[Signal[CC[O]]] = {
    import quotes.reflect.*

    matchSplitObservableExpr match {
      case '{
            SplitMatchSeqObservable.build[Self, I, K, O, CC](
              $keyFnExpr,
              $distinctComposeExpr,
              $duplicateKeysConfigExpr,
              $observableExpr
            )(
              ${caseExpr}*
            )(
              ${handlerExpr}*
            )
          } =>
        Expr.summon[Splittable[CC]] match {
          case None => report.errorAndAbort(
            "Macro expansion failed, cannot find Splittable instance of " + MacrosUtilities.ShowType.nameOf[CC]
          )
          case Some(splittableExpr) => {
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
              toSplittableSeqObservable(
                $observableExpr
                  .map { icc =>
                    $splittableExpr.map(
                      icc,
                      i => {
                        val (idx, b) = ${ innerObservableImpl('i, caseExprSeq) }
                        (i, idx, b)
                      }
                    )
                  }
                  .asInstanceOf[BaseObservable[Self, CC[(I, Int, Any)]]],
                $keyFnExpr,
                $distinctComposeExpr,
                $duplicateKeysConfigExpr,
                $splittableExpr,
                ${handlerExpr}*,
              )
            }
            }
          }
        }
      case _ =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

}
