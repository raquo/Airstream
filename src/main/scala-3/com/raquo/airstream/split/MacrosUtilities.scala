package com.raquo.airstream.split

import scala.quoted.{Expr, Quotes, Type}

private[split] object MacrosUtilities {

  def exprOfListToListOfExpr[T: Type](
    pfListExpr: Expr[List[T]]
  )(
    using quotes: Quotes
  ): List[Expr[T]] = {
    import quotes.reflect.*

    pfListExpr match {
      case '{ $headExpr :: (${ tailExpr }: List[T]) } =>
        headExpr :: exprOfListToListOfExpr(tailExpr)
      case '{ Nil } => Nil
      case _ =>
        report.errorAndAbort(
          "Macro expansion failed, please use `handleCase` instead"
        )
    }

  }

  def listOfExprToExprOfList[T: Type](
    pfExprList: List[Expr[T]]
  )(
    using quotes: Quotes
  ): Expr[List[T]] = {
    import quotes.reflect.*

    pfExprList match
      case head :: tail => '{ $head :: ${ listOfExprToExprOfList(tail) } }
      case Nil          => '{ Nil }
  }

  def innerObservableImpl[I: Type](
    iExpr: Expr[I],
    caseListExpr: Expr[List[PartialFunction[Any, Any]]]
  )(
    using quotes: Quotes
  ): Expr[(Int, Any)] = {
    import quotes.reflect.*

    val caseExprList = exprOfListToListOfExpr(caseListExpr)

    val allCaseDefLists = caseExprList.reverse.zipWithIndex
      .flatMap { case (caseExpr, idx) =>
        caseExpr.asTerm match {
          case Lambda(_, Match(_, caseDefList)) => {
            caseDefList.map { caseDef =>
              val idxExpr = Expr.apply(idx)
              val newRhsExpr = '{
                val res = ${ caseDef.rhs.asExprOf[Any] }; ($idxExpr, res)
              }
              CaseDef.copy(caseDef)(
                caseDef.pattern,
                caseDef.guard,
                newRhsExpr.asTerm
              )
            }
          }
          case _ =>
            report.errorAndAbort(
              "Macro expansion failed, please use `handleCase` with annonymous partial function"
            )
        }
      }
      .map(_.changeOwner(Symbol.spliceOwner))

    Match(iExpr.asTerm, allCaseDefLists).asExprOf[(Int, Any)]
  }

  object ShowType {
    def nameOfExpr[CC[_]](using Type[CC], Quotes): Expr[String] = Expr(Type.show[CC])
    inline def nameOf[CC[_]] = ${ nameOfExpr[CC] }
  }

}
