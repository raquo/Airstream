package com.raquo.airstream.split

import scala.quoted.{Expr, Quotes, Type}
import scala.annotation.tailrec

private[split] object MacrosUtilities {

  type CaseAny = Any
  type HandlerAny[+O] = Any

  def innerObservableImpl[I: Type](
    iExpr: Expr[I],
    caseExprSeq: Seq[Expr[CaseAny]]
  )(
    using quotes: Quotes
  ) = {
    import quotes.reflect.*

    @tailrec
    def getCaseDef(
      idx: Int,
      term: Term
    ): List[CaseDef] = {
      term match {
        case Inlined(_, _, inlinedTerm) => getCaseDef(idx, inlinedTerm)
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

    val allCaseDefLists = caseExprSeq.view
      .zipWithIndex
      .flatMap { case (caseExpr, idx) =>
        getCaseDef(idx, caseExpr.asTerm)
      }
      .map(_.changeOwner(Symbol.spliceOwner))
      .toList

    Match(iExpr.asTerm, allCaseDefLists).asExprOf[(Int, Any)]
  }

  object ShowType {
    def nameOfExpr[CC[_]](using Type[CC], Quotes): Expr[String] = Expr(Type.show[CC])
    inline def nameOf[CC[_]] = ${ nameOfExpr[CC] }
  }

}
