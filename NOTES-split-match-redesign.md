# Split-match redesign (major version)

Goal: replace the **chain-of-inline-macros** builder for `splitMatchOne` / `splitMatchSeq`
with a **single terminal macro** that takes all clauses as varargs. This removes the
phantom holder types, the per-step `delegate*` macros, and the two-step `.apply`, and
(crucially) makes handler parameter types infer normally in IntelliJ because the clause
factories become ordinary (non-inline) methods consumed by one macro.

See the conversation for the full design rationale.

## Target API

Old:
```scala
obs.splitMatchOne
  .handleCase { case Bar(Some(s)) => s } { sig => Res("Bar") }   // sig: StrictSignal[String]
  .handleType[Baz] { sig => Res("Baz") }
  .handleValue(Tar) { Res("Tar") }
  .toSignal            // or .toStream
```

New:
```scala
import com.raquo.airstream.split.MatchOneClause.*   // handleCase/handleType/handleValue/handleRest

obs.splitMatchOne(
  handleCase { case Bar(Some(s)) => s } { sig => Res("Bar") },
  handleType[Baz] { sig => Res("Baz") },
  handleValue(Tar) { Res("Tar") },
)   // : Signal[Res] (Self[O]); no .toSignal/.toStream
```

Seq analogous: `obs.splitMatchSeq(_.id)( handleCase{..}{..}, ... ) : Signal[CC[O]]`
(seq handlers get `Signal[_]`, not `StrictSignal[_]`).

## Key design decisions

- Clause type `MatchOneClause[I, +O]` (invariant `I`, covariant `O`). `MatchSeqClause[I, +O]` likewise.
  - `O` covariant so the varargs element type LUBs handler result types (union accumulation, as today).
  - `I` carried so `handleRest` can type its signal as `StrictSignal[I]`. Every factory takes
    `I` as a type param inferred **from the expected vararg element type** (= obs's `I`, concrete),
    NOT from its value args. `handleCase` keeps a free `A` for the PF so cross-type patterns
    (e.g. `case _: Int`) still compile as today.
- Factories are ordinary `@compileTimeOnly` methods (they never run; the macro rewrites their trees).
- The single macro `buildOne`/`buildSeq` extracts `(pfExpr, handlerExpr)` pairs from the clause
  varargs, then reuses the EXISTING proven machinery:
  - `MacrosUtilities.innerObservableImpl` to fuse the cases into one `match` (preserves
    exhaustiveness / unreachable-case warnings),
  - `toSplitOneObservable` / `toSplitSeqObservable` runtime dispatch (copied verbatim).
  - Synthesized case exprs are byte-identical in shape to what the old macros produced
    (`{ case x: T => x }`, `{ case _: V => v }`, `{ case r: I => r }`), so `getCaseDef` handles them.
- `handleType[T]` stays two-step (`handleType[T].apply(handler)`) — Scala can't take explicit `T`
  and infer `O` in one call — but `.apply` is now on a REAL class, which IntelliJ resolves reliably.

## Task list / progress

- [x] Write this note
- [x] Prototype type inference (no macros) to confirm `I`/`O` inference works — CONFIRMED (scala-cli, scratchpad/proto.scala): handleRest sig typed as I, handleCase as B, handleType as T, O = LUB, result Self[O]; cross-type `case _: Int` still compiles
- [x] `MatchOneClause.scala` (trait + factories)
- [x] `MatchSeqClause.scala` (trait + factories)
- [x] `SplitMatchMacros.scala` (buildOne, buildSeq, clause parsing, runtime helpers)
- [x] Trim `MacrosUtilities.scala` (drop MatchTypeHandler/MatchValueHandler; keep innerObservableImpl, ShowType)
- [x] Rewrite entry points in `ObservableMacroImplicits.scala`
- [x] Delete old files (6 holder types + 2 old macro objects)
- [x] Port `SplitMatchOneSpec` to new syntax (minimal diff)
- [x] Port `SplitMatchSeqSpec` to new syntax (minimal diff)
- [x] `Test/compile` on Scala 3 green
- [x] Run `SplitMatchOneSpec` + `SplitMatchSeqSpec` green (13/13)
- [x] Full `Test` suite green on Scala 3 (74 suites, 380 tests, 0 failed, 1 pre-existing ignored)
- [x] Confirm Scala 2.13 unaffected (split-match is Scala-3-only; `++2.13.18 Test/compile` green)
- [x] Gap tests: NOT adding compile-fail tests (suite has no such style; brittle). handleRest typing
      + exhaustiveness-satisfied are verified by the ported + handleRest specs compiling with only
      the one intentional warning.

## Status: COMPLETE

New single-macro `splitMatchOne` / `splitMatchSeq` implemented and green. Old chain-of-macros
design fully removed. Existing tests ported with minimal diffs (chain → varargs, same bodies &
assertions). The one intentional "match may not be exhaustive" test still warns as before,
confirming exhaustiveness checking is preserved.

### Follow-ups for the wider release (out of scope here)
- Update Laminar (separate repo) to the new call syntax; it re-exports these APIs.
- Consider re-exporting `MatchOneClause.*` / `MatchSeqClause.*` through Laminar's `L` so users get
  `handleCase` etc. in scope without an explicit import (watch the name clash between the One and
  Seq factory sets — only an issue if both are star-imported at once).
- CHANGELOG / migration note for the major version.
- Delete this NOTES file once the redesign is merged.

### Gotcha found & fixed (important for future macro edits)
Synthesized case exprs for handleType/handleValue/handleRest must get their PartialFunction
type from the **val's declared type** (`val e: Expr[PartialFunction[t,t]] = '{ { case x: t => x } }`),
NOT an in-quote ascription (`'{ { case x: t => x } : PartialFunction[t,t] }`). The in-quote
ascription produces a `Typed(Block(DefDef, Closure), _)` tree, and `MacrosUtilities.getCaseDef`
only matches the bare `Lambda` (= `Block(DefDef, Closure)`) form — so it aborts with
"please use handleCase with annonymous partial function". This matches how the old macros did it.

## Notes for resuming

- Entry points live in `core/ObservableMacroImplicits.scala` (mixed into `Observable`/`BaseObservable`
  companion via the existing trait wiring — unchanged).
- If inference ever fails at a call site, the fallback is an explicit type arg on the clause factory,
  or annotating the handler lambda param.
- Runtime helpers `toSplitOneObservable`/`toSplitSeqObservable` are `private[split]` in
  `SplitMatchMacros` and referenced from spliced code (same pattern as before).
- New tests still worth adding later: empty-clause-list compile error, and an EventStream source
  returning `EventStream[O]` directly (old `.toStream` path). Existing ported tests already cover
  signal+stream sources, handleCase/Type/Value/Rest, and exhaustiveness-warning cases.
