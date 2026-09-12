package com.raquo.airstream.split

import com.raquo.airstream.core.{BaseObservable, Observable, Signal}
import com.raquo.airstream.distinct.DistinctOps
import com.raquo.airstream.distinct.DistinctOps.DistinctOp
import com.raquo.airstream.split.MacrosUtilities.{CaseAny, HandlerAny, ShowType, innerObservableImpl}
import com.raquo.airstream.state.StrictSignal

import scala.quoted.{Expr, Quotes, Type, Varargs}
import scala.util.Success

/** The single macro behind `splitMatchOne` / `splitMatchSeq`.
  *
  * Each clause (see [[MatchOneClause]] / [[MatchSeqClause]]) is an ordinary `@compileTimeOnly`
  * factory call. This macro reads the clause trees, turns each into a `(caseExpr, handlerExpr)`
  * pair, fuses all the cases into a single `i match { ... => (idx, value) }` via
  * [[MacrosUtilities.innerObservableImpl]] (which is what preserves the compiler's exhaustiveness
  * and unreachable-case checking), and dispatches to the runtime `splitOne` / `splitSeq` helpers.
  *
  * The synthesized case expressions for `handleType` / `handleValue` / `handleRest` are the same
  * shape the pre-redesign macros produced, so `innerObservableImpl` handles them unchanged.
  */
object SplitMatchMacros {

  // ---------------------------------------------------------------------------
  // splitMatchOne
  // ---------------------------------------------------------------------------

  def buildOne[Self[+_] <: Observable[?]: Type, I: Type, O: Type](
    obsExpr: Expr[BaseObservable[Self, I]],
    clausesExpr: Expr[Seq[SplitMatchOneContext[I] => MatchOneClause[O]]]
  )(
    using quotes: Quotes
  ): Expr[Self[O]] = {
    import quotes.reflect.*

    val clauseExprs = unpackClauses(clausesExpr, "splitMatchOne")

    val (caseExprSeq, handlerExprSeq) =
      clauseExprs.map(clause => parseOneClause[I](clause)).unzip

    '{
      toSplitOneObservable[Self, O](
        $obsExpr
          .map(i => ${ innerObservableImpl[I]('i, caseExprSeq) })
          .asInstanceOf[BaseObservable[Self, (Int, Any)]],
        ${ Varargs(handlerExprSeq) }*
      )
    }
  }

  private def parseOneClause[I: Type](
    clause: Expr[SplitMatchOneContext[I] => MatchOneClause[?]]
  )(
    using quotes: Quotes
  ): (Expr[CaseAny], Expr[HandlerAny[Any]]) = {
    import quotes.reflect.*

    // Each clause is a lambda `ctx => ctx.handleXxx(...)`. Strip wrappers, the lambda, and then
    // destructure the body to find the `handleXxx` call and its pf / handler arguments.
    // pf and handler never reference `ctx`, but they are owned by the (discarded) clause lambda,
    // so they must be reowned to the splice owner before being moved into the generated tree.
    def unwrap(t: Term): Term = t match {
      case Inlined(_, _, inner) => unwrap(inner)
      case Block(Nil, inner) => unwrap(inner)
      case Typed(inner, _) => unwrap(inner)
      case _ => t
    }

    def reown(t: Term): Expr[Any] =
      t.changeOwner(Symbol.spliceOwner).asExprOf[Any]

    val body = unwrap(clause.asTerm) match {
      case Lambda(_, lambdaBody) => unwrap(lambdaBody)
      case other =>
        report.errorAndAbort(
          "`splitMatchOne` clauses must be written as `_.handleCase(...)(...)` etc.",
          other.pos
        )
    }

    // Recognize `_.handleType[T]` nested inside `.apply`, returning the T type tree.
    def handleTypeArg(fn: Term): Option[TypeTree] = fn match {
      case TypeApply(inner, _) => handleTypeArg(inner)
      case Select(TypeApply(Select(_, "handleType"), tTree :: Nil), "apply") => Some(tTree)
      case _ => None
    }

    body match {
      case Apply(Apply(TypeApply(Select(_, "handleCase"), _), pf :: Nil), handler :: Nil) =>
        (reown(pf).asExprOf[CaseAny], reown(handler).asExprOf[HandlerAny[Any]])

      case Apply(Apply(TypeApply(Select(_, "handleValue"), _), v :: Nil), handler :: Nil) =>
        v.tpe.asType match {
          case '[vt] =>
            val vExpr = v.changeOwner(Symbol.spliceOwner).asExprOf[vt]
            val handlerExpr = reown(handler)
            val caseExpr: Expr[PartialFunction[vt, vt]] = '{ { case _: vt => $vExpr } }
            val handlerFn = '{ (_: StrictSignal[vt]) => $handlerExpr }
            (caseExpr.asExprOf[CaseAny], handlerFn.asExprOf[HandlerAny[Any]])
        }

      case Apply(TypeApply(Select(_, "handleRest"), _), handler :: Nil) =>
        val caseExpr: Expr[PartialFunction[I, I]] = '{ { case rest: I => rest } }
        (caseExpr.asExprOf[CaseAny], reown(handler).asExprOf[HandlerAny[Any]])

      case Apply(applyFn, handler :: Nil) if handleTypeArg(applyFn).isDefined =>
        handleTypeArg(applyFn).get.tpe.asType match {
          case '[tt] =>
            val caseExpr: Expr[PartialFunction[tt, tt]] = '{ { case x: tt => x } }
            (caseExpr.asExprOf[CaseAny], reown(handler).asExprOf[HandlerAny[Any]])
        }

      case other =>
        report.errorAndAbort(
          "Unsupported `splitMatchOne` clause. Use `_.handleCase` / `_.handleType` / `_.handleValue` / `_.handleRest`.",
          other.pos
        )
    }
  }

  private def toSplitOneObservable[Self[+_] <: Observable[?], O](
    parentObservable: BaseObservable[Self, (Int, Any)],
    handlers: HandlerAny[O]*
  ): Self[O] = {
    parentObservable
      .splitOne(_._1) { dataSignal =>
        val idx = dataSignal.key
        val bSignal = dataSignal.map(_._2)
        handlers.view.zipWithIndex.map(_.swap).toMap
          .getOrElse(idx, throw new IllegalStateException("Illegal SplitMatchOne state. This is a bug in Airstream."))
          .asInstanceOf[Function1[Any, O]]
          .apply(bSignal)
      }
  }

  // ---------------------------------------------------------------------------
  // splitMatchSeq
  // ---------------------------------------------------------------------------

  def buildSeq[Self[+_] <: Observable[?]: Type, I: Type, K: Type, O: Type, CC[_]: Type](
    obsExpr: Expr[BaseObservable[Self, CC[I]]],
    keyFnExpr: Expr[I => K],
    distinctOpExpr: Expr[DistinctOp[I]],
    duplicateKeysConfigExpr: Expr[DuplicateKeysConfig],
    clausesExpr: Expr[Seq[SplitMatchSeqContext[I] => MatchSeqClause[O]]]
  )(
    using quotes: Quotes
  ): Expr[Signal[CC[O]]] = {
    import quotes.reflect.*

    Expr.summon[Splittable[CC]] match {
      case None =>
        report.errorAndAbort(
          "Macro expansion failed, cannot find Splittable instance of " + ShowType.nameOf[CC]
        )
      case Some(splittableExpr) =>
        val clauseExprs = unpackClauses(clausesExpr, "splitMatchSeq")

        val (caseExprSeq, handlerExprSeq) =
          clauseExprs.map(clause => parseSeqClause[I](clause)).unzip

        '{
          toSplitSeqObservable[Self, I, K, O, CC](
            $obsExpr
              .map { icc =>
                $splittableExpr.map(
                  icc,
                  i => {
                    val (idx, b) = ${ innerObservableImpl[I]('i, caseExprSeq) }
                    (i, idx, b)
                  }
                )
              }
              .asInstanceOf[BaseObservable[Self, CC[(I, Int, Any)]]],
            $keyFnExpr,
            $distinctOpExpr,
            $duplicateKeysConfigExpr,
            $splittableExpr,
            ${ Varargs(handlerExprSeq) }*
          )
        }
    }
  }

  private def parseSeqClause[I: Type](
    clause: Expr[SplitMatchSeqContext[I] => MatchSeqClause[?]]
  )(
    using quotes: Quotes
  ): (Expr[CaseAny], Expr[HandlerAny[Any]]) = {
    import quotes.reflect.*

    // Each clause is a lambda `ctx => ctx.handleXxx(...)`. Strip wrappers, the lambda, and then
    // destructure the body to find the `handleXxx` call and its pf / handler arguments.
    // pf and handler never reference `ctx`, but they are owned by the (discarded) clause lambda,
    // so they must be reowned to the splice owner before being moved into the generated tree.
    def unwrap(t: Term): Term = t match {
      case Inlined(_, _, inner) => unwrap(inner)
      case Block(Nil, inner) => unwrap(inner)
      case Typed(inner, _) => unwrap(inner)
      case _ => t
    }

    def reown(t: Term): Expr[Any] =
      t.changeOwner(Symbol.spliceOwner).asExprOf[Any]

    val body = unwrap(clause.asTerm) match {
      case Lambda(_, lambdaBody) => unwrap(lambdaBody)
      case other =>
        report.errorAndAbort(
          "`splitMatchSeq` clauses must be written as `_.handleCase(...)(...)` etc.",
          other.pos
        )
    }

    // Recognize `_.handleType[T]` nested inside `.apply`, returning the T type tree.
    def handleTypeArg(fn: Term): Option[TypeTree] = fn match {
      case TypeApply(inner, _) => handleTypeArg(inner)
      case Select(TypeApply(Select(_, "handleType"), tTree :: Nil), "apply") => Some(tTree)
      case _ => None
    }

    body match {
      case Apply(Apply(TypeApply(Select(_, "handleCase"), _), pf :: Nil), handler :: Nil) =>
        (reown(pf).asExprOf[CaseAny], reown(handler).asExprOf[HandlerAny[Any]])

      case Apply(Apply(TypeApply(Select(_, "handleValue"), _), v :: Nil), handler :: Nil) =>
        v.tpe.asType match {
          case '[vt] =>
            val vExpr = v.changeOwner(Symbol.spliceOwner).asExprOf[vt]
            val handlerExpr = reown(handler)
            val caseExpr: Expr[PartialFunction[vt, vt]] = '{ { case _: vt => $vExpr } }
            val handlerFn = '{ (_: Signal[vt]) => $handlerExpr }
            (caseExpr.asExprOf[CaseAny], handlerFn.asExprOf[HandlerAny[Any]])
        }

      case Apply(TypeApply(Select(_, "handleRest"), _), handler :: Nil) =>
        val caseExpr: Expr[PartialFunction[I, I]] = '{ { case rest: I => rest } }
        (caseExpr.asExprOf[CaseAny], reown(handler).asExprOf[HandlerAny[Any]])

      case Apply(applyFn, handler :: Nil) if handleTypeArg(applyFn).isDefined =>
        handleTypeArg(applyFn).get.tpe.asType match {
          case '[tt] =>
            val caseExpr: Expr[PartialFunction[tt, tt]] = '{ { case x: tt => x } }
            (caseExpr.asExprOf[CaseAny], reown(handler).asExprOf[HandlerAny[Any]])
        }

      case other =>
        report.errorAndAbort(
          "Unsupported `splitMatchSeq` clause. Use `_.handleCase` / `_.handleType` / `_.handleValue` / `_.handleRest`.",
          other.pos
        )
    }
  }

  private inline def wrappedDistinctCompose[K, I](
    distinctOp: DistinctOp[I]
  ): DistinctOp[(I, Int, Any)] = {
    DistinctOp[(I, Int, Any)] { ops =>
      val scopedOps = new DistinctOps.Ops[I]
      ops.distinctTry { (prevTry, nextTry) =>
        distinctOp(scopedOps)(prevTry.map(_._1), nextTry.map(_._1))
        && {
          (prevTry, nextTry) match {
            case (Success(prev), Success(next)) =>
              prev._2 == next._2 && prev._3 == next._3
            case _ =>
              false
          }
        }
      }
    }
  }

  private inline def customKey[I, K](
    keyFn: I => K
  )(
    input: (I, Int, Any)
  ): (Int, K) = {
    val (i, idx, _) = input
    idx -> keyFn(i)
  }

  private def toSplitSeqObservable[Self[+_] <: Observable[?], I, K, O, CC[_]](
    parentObservable: BaseObservable[Self, CC[(I, Int, Any)]],
    keyFn: I => K,
    distinctOp: DistinctOp[I],
    duplicateKeysConfig: DuplicateKeysConfig,
    splittable: Splittable[CC],
    handlers: HandlerAny[O]*
  ): Signal[CC[O]] = {
    parentObservable.splitSeq(
      key = customKey(keyFn),
      distinctOp = wrappedDistinctCompose[K, I](distinctOp),
      duplicateKeys = duplicateKeysConfig
    ) { dataSignal =>
      val idx = dataSignal.key._1
      val bSignal = dataSignal.map(_._3)
      handlers.view.zipWithIndex.map(_.swap).toMap
        .getOrElse(idx, throw new IllegalStateException("Illegal SplitMatchSeq state. This is a bug in Airstream."))
        .asInstanceOf[Function1[Any, O]]
        .apply(bSignal)
    }(splittable)
  }

  // ---------------------------------------------------------------------------

  private def unpackClauses[A](
    clausesExpr: Expr[Seq[A]],
    methodName: String
  )(
    using quotes: Quotes
  ): Seq[Expr[A]] = {
    import quotes.reflect.*
    clausesExpr match {
      case Varargs(exprs) if exprs.nonEmpty => exprs
      case Varargs(_) =>
        report.errorAndAbort(s"`$methodName` requires at least one clause.")
      case _ =>
        report.errorAndAbort(
          s"`$methodName` requires its clauses to be passed directly as arguments."
        )
    }
  }

}
