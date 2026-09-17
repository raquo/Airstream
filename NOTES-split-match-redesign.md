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

New (final `_.` context syntax — no import needed):
```scala
obs.splitMatchOne(
  _.handleCase { case Bar(Some(s)) => s } { sig => Res("Bar") },
  _.handleType[Baz] { sig => Res("Baz") },
  _.handleValue(Tar) { Res("Tar") },
)   // : Signal[Res] (Self[O]); no .toSignal/.toStream
```

Seq analogous: `obs.splitMatchSeq(_.id)( _.handleCase{..}{..}, ... ) : Signal[CC[O]]`
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

## RESULT: SUCCESS ✅ (One AND Seq both on `_.` context syntax)

Both `splitMatchOne` and `splitMatchSeq` ported to `_.handleXxx(...)` context syntax end-to-end.
- SplitMatchOneSpec 6/6 + SplitMatchSeqSpec 7/7 green (13/13).
- Full Scala 3 suite green (74 suites, 380 tests, 0 failed, 1 pre-existing ignored).
- Scala 2.13 `Test/compile` green (split-match is Scala-3-only; confirms nothing leaked).
- Only warning in the whole build is the ONE intentional non-exhaustive-match test in SplitMatchOneSpec.

Validated: lambda deconstruction, owner fixing (`changeOwner(Symbol.spliceOwner)` on extracted
pf/handler), exhaustiveness warning still fires, handler param inference, O = LUB, 100-case compiles.

Type-precision tests added (One + Seq specs, "result type: ..."): ascribe the result to lock
(a) O is the exact LUB of handler result types and does NOT silently widen to `Any` (covariance
makes the ascription load-bearing — verified by a temporary wrong ascription failing to compile,
with the expected type even propagating inward to the handler bodies), (b) `Self` is preserved for
One (`Signal` source → `Signal[O]`, `EventStream` → `EventStream[O]`), (c) `CC` is preserved
precisely for Seq (`List` source → `Signal[List[O]]`, `Vector` → `Signal[Vector[O]]`). Both a
homogeneous case (exact O) and a heterogeneous case (LUB `<: Foo`) are covered.

Macro-internal note: extracting clause args from inside the lambda needs reflect-level Term matching
(not quote patterns), and the `reown` helper returns `Expr[Any]` (can't be generic — `asExprOf[A]`
needs a `Type[A]`; the typed `v` in handleValue is reowned inline where `vt` is in scope).
`parseSeqClause` is a near-verbatim copy of `parseOneClause`; the ONLY difference is the
`handleValue` handler wrapper — Seq wraps as `(_: Signal[vt]) => ...`, One as `(_: StrictSignal[vt]) => ...`
(matching the respective context method signatures).

Design now symmetric:
- `MatchOneClause[+O]` + `SplitMatchOneContext[I]` (handlers `StrictSignal[_]`); entry
  `splitMatchOne[O](clauses: (SplitMatchOneContext[I] => MatchOneClause[O])*)`.
- `MatchSeqClause[+O]` + `SplitMatchSeqContext[I]` (handlers `Signal[_]`); entry
  `splitMatchSeq[O](keyFn, distinctOp, dupKeysConfig)(clauses: (SplitMatchSeqContext[I] => MatchSeqClause[O])*)`.
- Each context exposes `handleCase` / `handleType[T]` (two-step `.apply` via inner `HandleType[I, T]`) /
  `handleValue` / `handleRest`, all `@compileTimeOnly` stubs.

## RESOLVED: `handleValue` singleton guard (2026-09-12) ✅

`requireSingleton` added to the `handleValue` branch of both `parseOneClause` and `parseSeqClause`
in `SplitMatchMacros.scala`. Regression tests in `SplitMatchHandleValueSpec.scala` (accepts proven
by real compiling code; rejects by `assertDoesNotCompile`, each paired with a compiling `handleCase`
alternative). Full Scala 3 suite (386) green + Scala 2.13 `Test/compile` clean; only the one
intentional exhaustiveness warning remains.

### Final rule (STRICTER than the originally-leaned #1 below — see why)
Accept `handleValue(v)` **only when `v` is a statically-known singleton LITERAL**: a case object, a
plain `object`, or an enum case. Predicate on the reflect term symbol of the arg:
```scala
val sym = v.tpe.termSymbol
sym.flags.is(Flags.Module) || (sym.flags.is(Flags.Enum) && sym.flags.is(Flags.Case))
```
Everything else is rejected with a user-facing error pointing at the arg (a `val`/`def`/`var`
reference, a constructor call, a literal, an instance), directing users to `handleCase { case
\`x\` => x } { ... }` for value-equality matching.

### Why stricter than `v.tpe <:< scala.Singleton` (the key empirical finding)
The originally-leaned `<:< scala.Singleton` check is TOO LENIENT. A `val` reference's path type
(`x.type`) is itself a singleton type, so `<:< Singleton` is `true` even for `val x: Foo = Tar`.
Probes established the real boundary is **exhaustiveness participation**, and:
- literal case object / `object` / enum case → generated `case _: v.type` uses the precise
  statically-known singleton → **participates in exhaustiveness**. ✅
- `val x: Foo = Tar` (widened) → `case _: x.type` compiles and matches only Tar at runtime, but does
  NOT participate in exhaustiveness (compiler warns "case Tar not covered"). Silent footgun. ❌
- `val x: Tar.type = Tar` / inferred `val x = Tar` → DO participate (Scala keeps the singleton type),
  but a stray ascription silently breaks that — fragile.
- **Enum-typed vals are reflectively indistinguishable:** for `val x: Baz = Baz.Baz1`, `val x:
  Baz.Baz1.type = Baz.Baz1`, AND literal `Baz.Baz1`, `v.tpe.widen` is `Baz` in every case, and there
  is NO reflect op that recovers `Baz.Baz1` from a `val`'s static type. `widenTermRefByName` also
  collapses to `Baz`. So "reject only the widened vals" is **impossible to implement correctly** for
  enum vals. Hence the clean line: require the singleton to be passed literally. `v.tpe.termSymbol`
  flags cleanly separate literal modules/enum-cases (Module / Enum&Case) from all `val` refs (all
  flags false).

This intentionally also rejects `val x = Tar; handleValue(x)` (which would technically work) in
exchange for a predictable, non-fragile rule that matches the macro's exhaustiveness purpose. Users
inline to `handleValue(Tar)` or use `handleCase`.

---
## (historical) OPEN ISSUE: `handleValue` matching semantics (investigated 2026-09-12)

### Status quo (verified against git `666a958^` + empirical probes)
- `handleValue` has NEVER compared by `==`. Both the old chain-macros and the new single macro
  synthesize a **type test**: `'{ { case _: V => $v } }` (old `handleValueApplyImpl`, new
  `parseOneClause`/`parseSeqClause`). This is unlike Waypoint's `SplitRender.collectStatic`, which
  uses a stable-id pattern `case \`page\`` → real `==` value equality.
- It behaves like a value match ONLY because `V` is meant to be a singleton type (one inhabitant,
  so type-test ≡ value match). The OLD code guaranteed that with `(using inline valueOf: ValueOf[V])`
  on `delegateHandleValue` — `ValueOf[V]` exists only for singleton types, so it (a) pinned `V` to the
  precise singleton and (b) rejected non-singleton args at compile time.
- **The refactor DROPPED the `ValueOf[V]` bound.** `handleValue[V, O](v: V)(handler: => O)` has no
  singleton constraint anymore.

### Consequences (probes, One macro; Seq identical)
- `handleValue(Tar)` (case object) → reflect `v.tpe` = `Tar.type` (singleton) → `case _: Tar.type` →
  ✅ correct, and satisfies exhaustiveness. All current tests pass only because they pass case objects.
- `handleValue(Baz.Baz1)` / parameterized `handleValue(Qux.Qux1)` (ENUM singletons) → reflect `v.tpe`
  gives the precise `Baz.Baz1.type` / `Qux.Qux1.type` (NOT the widened enum type) → ✅ matches only
  that case, exhaustiveness fine. The anticipated enum/singleton-widening trouble does NOT manifest
  here because the macro reads the reflect `TermRef`, not the surface-inferred (often-widened) type.
- `handleValue(stableVal)` (a `val` of a wider type) → `v.tpe` = `stableVal.type` → `case _: x.type`
  → reference/`eq` identity test (matches only that exact instance). Edge case; old code same.
- `handleValue(mkTar())` (NON-stable expr, `mkTar(): Foo`) → `v.tpe` WIDENS to `Foo` → `case _: Foo`
  → ❌ matches EVERYTHING, silently, and COMPILES. This is the real regression: old code rejected it
  (no `ValueOf[Foo]`).

### Fix options
1. **Macro guard (RECOMMENDED, validated):** in the `handleValue` branch of parseOneClause/parseSeqClause,
   `if (!(v.tpe <:< TypeRepr.of[scala.Singleton])) report.errorAndAbort(...)`. NOTE: must be
   `scala.Singleton` — bare `Singleton` resolves to `quotes.reflect.Singleton` and fails with an E202
   staging error. Prototyped end-to-end: rejects `mkTar()` (points at the arg), accepts case objects +
   simple & parameterized enum singletons; full split specs stayed green (17 tests). No API change, no
   implicit param, no clause-tree-shape change. Keeps current (correct) exhaustiveness behavior.
   Then add: a compile-success test for enum-singleton `handleValue`, and (since the suite has no
   compile-fail harness) at least a comment documenting the rejected widened case.
2. Restore `using ValueOf[V]` — faithful to old contract but adds an implicit arg list, changing the
   clause tree shape, so the `Apply(Apply(TypeApply(Select(_,"handleValue"),_), v::Nil), handler::Nil)`
   matcher would need to peel the extra `Apply`. More churn than #1.
3. Genuine `case \`v\`` stable-id pattern (Waypoint-style `==`) — would also work for non-singletons,
   but: (a) can't splice a value into pattern position via quotes for construction, so it needs hand-
   building a `Lambda(_, Match(_, CaseDef(vRef, None, rhs)))` tree that `getCaseDef` accepts — real
   surgery; (b) a value guard degrades exhaustiveness; stable-id-ref patterns keep it, but only for
   stable refs (same population as #1 anyway). Not worth it — users wanting value-`==` on a non-singleton
   can already write `_.handleCase { case \`x\` => x } { ... }`.

Decision: implemented a STRICTER variant of #1 (literal-singleton only, via `termSymbol` flags rather
than `<:< scala.Singleton`). See the RESOLVED section above for why the plain `<:< Singleton` check
was insufficient.

## (superseded) SPIKE PLAN — `_.` context-object syntax

User wants `obs.splitMatchOne(_.handleCase{..}{..}, _.handleType[Baz]{..}, ...)` so that:
- no import is needed (handle* are members of a context object reached via `_.`)
- IDE autocompletes after `_.`
- One vs Seq `handleValue` etc. don't clash (different context types)

Design: clauses become `(SplitMatchOneContext[I] => MatchOneClause[O])*`. handle* move onto
`SplitMatchOneContext[I]` (compileTimeOnly stubs). `MatchOneClause` drops its `I` param → `[+O]`.
The macro strips one `Lambda(param, body)` per clause, then destructures `body`
(`ctx.handleCase(pf)(handler)` etc.) at the reflect Term level, and **reowns extracted pf/handler
to `Symbol.spliceOwner`** (they were owned by the discarded clause-lambda anonfun).

Spiking `splitMatchOne` (One) ONLY first; Seq left on the old import-style until the spike validates
the lambda/owner handling. If the spike works, port Seq the same way.

Files touched by spike: MatchOneClause.scala (adds SplitMatchOneContext), SplitMatchMacros.scala
(buildOne + parseOneClause rewritten), ObservableMacroImplicits.scala (One entry), SplitMatchOneSpec
(prefix clauses with `_.`, drop the MatchOneClause.* import).

## Status of earlier (import-style) impl: COMPLETE, now being superseded by spike

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
