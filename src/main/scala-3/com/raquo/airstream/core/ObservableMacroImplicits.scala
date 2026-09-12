package com.raquo.airstream.core

import com.raquo.airstream.core.{Observable, BaseObservable, Signal}
import com.raquo.airstream.distinct.DistinctOps
import com.raquo.airstream.distinct.DistinctOps.DistinctOp
import com.raquo.airstream.split.*
import com.raquo.airstream.state.StrictSignal

trait ObservableMacroImplicits {

  extension [Self[+_] <: Observable[?], I](inline observable: BaseObservable[Self, I]) {

    /** Split an observable by matching each value against a set of clauses, each of which renders
      * its matched value into an `O`. The clauses are built with [[MatchOneClause]]'s factories:
      *
      * {{{
      * fooSignal.splitMatchOne(
      *   _.handleCase { case Bar(Some(str)) => str } { strSignal => renderBar(strSignal) },
      *   _.handleType[Baz] { bazSignal => renderBaz(bazSignal) },
      *   _.handleValue(Tar) { renderTar() },
      *   _.handleRest { fooSignal => renderFallback(fooSignal) },
      * )
      * }}}
      *
      * Each clause is written `_.handleXxx(...)`, where `_` is a [[SplitMatchOneContext]].
      * The macros fuse the clauses into a single pattern match, so the compiler reports the usual
      * "match may not be exhaustive" / "unreachable case" warnings. The result type is `Self[O]`,
      * where `O` is the least upper bound of the clauses' handler result types – so a `Signal`
      * input yields a `Signal[O]` and an `EventStream` an `EventStream[O]`.
      *
      * The signals provided in the callbacks are [[StrictSignal]]-s, so you can read .now() from them.
      */
    inline def splitMatchOne[O](inline clauses: (SplitMatchOneContext[I] => MatchOneClause[O])*): Self[O] =
      ${ SplitMatchMacros.buildOne[Self, I, O]('observable, 'clauses) }
  }

  extension [Self[+_] <: Observable[?], I, K, CC[_]](inline observable: BaseObservable[Self, CC[I]]) {

    /** Split a collection-valued observable by key, matching each element against a set of clauses.
      * The clauses are built with [[SplitMatchSeqContext]]'s `handle*` methods (handlers receive a
      * plain `Signal[_]`):
      *
      * {{{
      * fooListSignal.splitMatchSeq(_.id)(
      *   _.handleCase { case FooE(Some(num)) => num } { numSignal => renderNum(numSignal) },
      *   _.handleType[FooC] { fooCSignal => renderFooC(fooCSignal) },
      *   _.handleValue(FooO) { renderFooO() },
      *   _.handleRest { fooSignal => renderFallback(fooSignal) },
      * )
      * }}}
      *
      * Each clause is written `_.handleXxx(...)`, where `_` is a [[SplitMatchSeqContext]].
      * The result type is `Signal[CC[O]]` regardless of the input observable type.
      *
      * The signals provided in the callbacks are [[StrictSignal]]-s, so you can read .now() from them.
      */
    inline def splitMatchSeq[O](
      inline keyFn: Function1[I, K],
      inline distinctOp: DistinctOp[I] = (ops: DistinctOps.Ops[I]) => ops.distinct,
      inline duplicateKeysConfig: DuplicateKeysConfig = DuplicateKeysConfig.default,
    )(
      inline clauses: (SplitMatchSeqContext[I] => MatchSeqClause[O])*
    ): Signal[CC[O]] =
      ${ SplitMatchMacros.buildSeq[Self, I, K, O, CC]('observable, 'keyFn, 'distinctOp, 'duplicateKeysConfig, 'clauses) }
  }
}
