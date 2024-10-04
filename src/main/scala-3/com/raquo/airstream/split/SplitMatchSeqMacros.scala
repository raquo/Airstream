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

object SplitMatchSeqMacros {
  
  extension [Self[+_] <: Observable[_], I, K, CC[_]](inline observable: BaseObservable[Self, CC[I]]) {
    inline def splitMatchSeq(
      inline keyFn: Function1[I, K],
      inline distinctCompose: Function1[Signal[I], Signal[I]] = (iSignal: Signal[I]) => iSignal.distinct,
      inline duplicateKeysConfig: DuplicateKeysConfig = DuplicateKeysConfig.default,
    ) = {
      SplitMatchSeqObservable.build(
        keyFn,
        distinctCompose,
        duplicateKeysConfig,
        observable,
        Nil,
        Map.empty[Int, Function2[Any, Any, Nothing]]
      )
    }
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_]](
    inline matchSplitObservable: SplitMatchSeqObservable[Self, I, K, O, CC]
  ) {
    inline def handleCase[A, B, O1 >: O](inline casePf: PartialFunction[A, B])(inline handleFn: (B, Signal[B]) => O1) = ${
      handleCaseImpl('{ matchSplitObservable }, '{ casePf }, '{ handleFn })
    }

    inline private def handlePfType[T](inline casePf: PartialFunction[Any, T]) = ${
      handleTypeImpl('{ matchSplitObservable }, '{ casePf })
    }

    inline def handleType[T]: SplitMatchSeqTypeObservable[Self, I, K, O, CC, T] = handlePfType[T] { case t: T => t }

    inline private def handlePfValue[V](inline casePf: PartialFunction[Any, V]) = ${
      handleValueImpl('{ matchSplitObservable }, '{ casePf })
    }

    inline def handleValue[V](inline v: V)(using inline valueOf: ValueOf[V]): SplitMatchSeqValueObservable[Self, I, K, O, CC, V] = handlePfValue[V] { case _: V => v }

    inline def toSignal: Signal[CC[O]] = ${ observableImpl('{ matchSplitObservable }) }
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_], T](inline matchTypeObserver: SplitMatchSeqTypeObservable[Self, I, K, O, CC, T]) {
    inline def apply[O1 >: O](inline handleFn: (T, Signal[T]) => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] = ${
      handleTypeApplyImpl('{ matchTypeObserver }, '{ handleFn })
    }
  }

  extension [Self[+_] <: Observable[_], I, K, O, CC[_], V](inline matchValueObservable: SplitMatchSeqValueObservable[Self, I, K, O, CC, V]) {
    inline private def deglate[O1 >: O](inline handleFn: (V, Signal[V]) => O1) = ${
      handleValueApplyImpl('{ matchValueObservable }, '{ handleFn })
    }

    inline def apply[O1 >: O](inline handleFn: Signal[V] => O1): SplitMatchSeqObservable[Self, I, K, O1, CC] = deglate { (_, vSignal) => handleFn(vSignal) }
  }

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
              $observableExpr,
              $caseListExpr,
              $handlerMapExpr
            )
          } =>
        innerHandleCaseImpl(
          keyFnExpr,
          distinctComposeExpr,
          duplicateKeysConfigExpr,
          observableExpr,
          caseListExpr,
          handlerMapExpr,
          casePfExpr,
          handleFnExpr
        )
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

  private def handleTypeImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, CC[_]: Type, T: Type](
    matchSplitObservableExpr: Expr[SplitMatchSeqObservable[Self, I, K, O, CC]],
    casePfExpr: Expr[PartialFunction[T, T]]
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
              $observableExpr,
              $caseListExpr,
              $handlerMapExpr
            )
          } =>
        '{
          SplitMatchSeqTypeObservable.build[Self, I, K, O, CC, T](
            $keyFnExpr,
            $distinctComposeExpr,
            $duplicateKeysConfigExpr,
            $observableExpr,
            $caseListExpr,
            $handlerMapExpr,
            $casePfExpr
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
              $observableExpr,
              $caseListExpr,
              $handlerMapExpr,
              $tCaseExpr
            )
          } =>
        innerHandleCaseImpl(
          keyFnExpr,
          distinctComposeExpr,
          duplicateKeysConfigExpr,
          observableExpr,
          caseListExpr,
          handlerMapExpr,
          tCaseExpr,
          handleFnExpr
        )
      case other =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

  private def handleValueImpl[Self[+_] <: Observable[_]: Type, I: Type, K: Type, O: Type, CC[_]: Type, V: Type](
    matchSplitObservableExpr: Expr[SplitMatchSeqObservable[Self, I, K, O, CC]],
    casePfExpr: Expr[PartialFunction[V, V]]
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
              $observableExpr,
              $caseListExpr,
              $handlerMapExpr
            )
          } =>
        '{
          SplitMatchSeqValueObservable.build[Self, I, K, O, CC, V](
            $keyFnExpr,
            $distinctComposeExpr,
            $duplicateKeysConfigExpr,
            $observableExpr,
            $caseListExpr,
            $handlerMapExpr,
            $casePfExpr
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
              $observableExpr,
              $caseListExpr,
              $handlerMapExpr,
              $tCaseExpr
            )
          } =>
        innerHandleCaseImpl(
          keyFnExpr,
          distinctComposeExpr,
          duplicateKeysConfigExpr,
          observableExpr,
          caseListExpr,
          handlerMapExpr,
          tCaseExpr,
          handleFnExpr
        )
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
    caseListExpr: Expr[List[PartialFunction[Any, Any]]],
    handlerMapExpr: Expr[Map[Int, Function2[Any, Any, O]]],
    casePfExpr: Expr[PartialFunction[A, B]],
    handleFnExpr: Expr[Function2[B, Signal[B], O1]]
  )(
    using quotes: Quotes
  ): Expr[SplitMatchSeqObservable[Self, I, K, O1, CC]] = {
    import quotes.reflect.*

    val caseExprList = MacrosUtilities.exprOfListToListOfExpr(caseListExpr)

    val nextCaseExprList =
      casePfExpr.asExprOf[PartialFunction[Any, Any]] :: caseExprList

    val nextCaseListExpr = MacrosUtilities.listOfExprToExprOfList(nextCaseExprList)

    '{
      SplitMatchSeqObservable.build[Self, I, K, O1, CC](
        $keyFnExpr,
        $distinctComposeExpr,
        $duplicateKeysConfigExpr,
        $observableExpr,
        $nextCaseListExpr,
        ($handlerMapExpr + ($handlerMapExpr.size -> $handleFnExpr
          .asInstanceOf[Function2[Any, Any, O1]]))
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

  private inline def toSplittableSeqObservable[Self[+_] <: Observable[_], I, K, O, CC[_]](
    parentObservable: BaseObservable[Self, CC[(I, Int, Any)]],
    keyFn: I => K,
    distinctCompose: Signal[I] => Signal[I],
    duplicateKeysConfig: DuplicateKeysConfig,
    handlerMap: Map[Int, Function2[Any, Any, O]],
    splittable: Splittable[CC]
  ): Signal[CC[O]] = {
    parentObservable
      .matchStreamOrSignal(
        ifStream = _.split(
          key = customKey(keyFn),
          distinctCompose = customDistinctCompose(distinctCompose),
          duplicateKeys = duplicateKeysConfig
        ) { case ((idx, _), (_, _, b), dataSignal) =>
          val bSignal = dataSignal.map(_._3)
          handlerMap.apply(idx).apply(b, bSignal)
        }(splittable),
        ifSignal = _.split(
          key = customKey(keyFn),
          distinctCompose = customDistinctCompose(distinctCompose),
          duplicateKeys = duplicateKeysConfig
        ) { case ((idx, _), (_, _, b), dataSignal) =>
          val bSignal = dataSignal.map(_._3)
          handlerMap.apply(idx).apply(b, bSignal)
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
      case '{ SplitMatchSeqObservable.build[Self, I, K, O, CC]($_, $_, $_, $_, Nil, $_) } =>
        report.errorAndAbort(
          "Macro expansion failed, need at least one handleCase"
        )
      case '{
            SplitMatchSeqObservable.build[Self, I, K, O, CC](
              $keyFnExpr,
              $distinctComposeExpr,
              $duplicateKeysConfigExpr,
              $observableExpr,
              $caseListExpr,
              $handlerMapExpr
            )
          } =>
        Expr.summon[Splittable[CC]] match {
          case None => report.errorAndAbort(
            "Macro expansion failed, cannot find Splittable instance of " + MacrosUtilities.ShowType.nameOf[CC]
          )
          case Some(splittableExpr) =>
            '{
              toSplittableSeqObservable(
                $observableExpr
                  .map { icc =>
                    $splittableExpr.map(
                      icc,
                      i => {
                        val (idx, b) = ${ MacrosUtilities.innerObservableImpl('i, caseListExpr) }
                        (i, idx, b)
                      }
                    )
                  }
                  .asInstanceOf[BaseObservable[Self, CC[(I, Int, Any)]]],
                $keyFnExpr,
                $distinctComposeExpr,
                $duplicateKeysConfigExpr,
                $handlerMapExpr,
                $splittableExpr
              )
            }
        }
      case _ =>
        report.errorAndAbort(
          "Macro expansion failed, please use `splitMatchSeq` instead of creating new SplitMatchSeqObservable explicitly"
        )
    }
  }

}
