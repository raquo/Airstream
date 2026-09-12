package com.raquo.airstream.split

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var

/** Regression tests for the `handleValue` singleton guard (see `SplitMatchMacros.requireSingleton`).
  *
  * `handleValue(v)` compiles to a generated `case _: v.type` type test. That only behaves as a
  * value match – and participates in the compiler's exhaustiveness checking, which is the whole
  * point of these macros – when `v` is a statically-known singleton: a case object, a plain
  * `object`, or an enum case. For anything else (a `val`/`def`/`var` reference, a constructor call,
  * a literal, an instance) the static type is wider than one value, so the macro rejects it.
  *
  * IMPORTANT nuance these tests pin down: a `val` reference is rejected EVEN WHEN it holds a
  * singleton (e.g. `val x: Baz = Baz.Baz1`, or even `val x: Baz.Baz1.type = Baz.Baz1`). At the macro
  * level there is no reflect operation that recovers `Baz.Baz1` from a `val` of static type `Baz`,
  * and a `val` of singleton type does not participate in exhaustiveness the way the literal case
  * does – so we require the singleton to be passed literally. The accepted forms are proven by
  * ordinary compilation; the rejected forms by `assertDoesNotCompile`, each paired with a compiling
  * `handleCase` alternative so the snippet can only fail for the intended reason.
  */
class SplitMatchHandleValueSpec extends UnitSpec {

  sealed trait Foo
  final case class Bar(s: String) extends Foo
  case object Tar extends Foo
  object Standalone extends Foo
  enum Baz extends Foo { case Baz1, Baz2 }
  enum Qux(val n: Int) extends Foo { case Qux1 extends Qux(1); case Qux2 extends Qux(2) }

  val fooVar: Var[Foo] = Var[Foo](Tar)
  val fooListVar: Var[List[Foo]] = Var[List[Foo]](List(Tar))
  val anyVar: Var[Any] = Var[Any](Tar)

  it("accepts statically-known singletons (splitMatchOne)") {
    // Each of these is real code: if a valid singleton form stopped compiling, this test would fail
    // to compile. `handleRest` keeps every match exhaustive, so there are no spurious warnings.

    val caseObject: Signal[Foo] = fooVar.signal.splitMatchOne(
      _.handleValue(Tar) { Tar },
      _.handleRest { _ => Tar },
    )

    val plainObject: Signal[Foo] = fooVar.signal.splitMatchOne(
      _.handleValue(Standalone) { Tar },
      _.handleRest { _ => Tar },
    )

    val simpleEnumCase: Signal[Foo] = fooVar.signal.splitMatchOne(
      _.handleValue(Baz.Baz1) { Tar },
      _.handleValue(Baz.Baz2) { Tar },
      _.handleRest { _ => Tar },
    )

    val parameterizedEnumCase: Signal[Foo] = fooVar.signal.splitMatchOne(
      _.handleValue(Qux.Qux1) { Tar },
      _.handleValue(Qux.Qux2) { Tar },
      _.handleRest { _ => Tar },
    )

    // A `handleValue` over the full sealed hierarchy is recognized as exhaustive (no `handleRest`):
    val exhaustive: Signal[Foo] = fooVar.signal.splitMatchOne(
      _.handleCase { case Bar(s) => s } { _ => Tar },
      _.handleType[Baz] { _ => Tar },
      _.handleType[Qux] { _ => Tar },
      _.handleValue(Standalone) { Tar },
      _.handleValue(Tar) { Tar },
    )

    assert(caseObject != null && plainObject != null && simpleEnumCase != null)
    assert(parameterizedEnumCase != null && exhaustive != null)
  }

  it("accepts statically-known singletons (splitMatchSeq)") {
    val seq: Signal[List[Foo]] = fooListVar.signal.splitMatchSeq(_.toString)(
      _.handleValue(Tar) { Tar },
      _.handleValue(Baz.Baz1) { Tar },
      _.handleRest { _ => Tar },
    )
    assert(seq != null)
  }

  it("rejects non-singletons, and the handleCase alternative compiles (splitMatchOne)") {

    // --- a `val` whose static type is the wide trait (the headline footgun) ---
    assertDoesNotCompile(
      """val x: Foo = Tar
        |fooVar.signal.splitMatchOne(_.handleValue(x) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )
    // ...the equivalent value match via handleCase DOES compile:
    assertCompiles(
      """val x: Foo = Tar
        |fooVar.signal.splitMatchOne(_.handleCase { case `x` => x } { _ => Tar }, _.handleRest { _ => Tar })""".stripMargin
    )

    // --- a `val` of an enum type (the `val c = Color.Red; handleValue(c)` case) ---
    assertDoesNotCompile(
      """val c: Baz = Baz.Baz1
        |fooVar.signal.splitMatchOne(_.handleValue(c) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )
    assertCompiles(
      """val c: Baz = Baz.Baz1
        |fooVar.signal.splitMatchOne(_.handleCase { case `c` => c } { _ => Tar }, _.handleRest { _ => Tar })""".stripMargin
    )

    // --- a `val` of explicit singleton type: still rejected (no exhaustiveness participation) ---
    assertDoesNotCompile(
      """val x: Tar.type = Tar
        |fooVar.signal.splitMatchOne(_.handleValue(x) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )
    assertDoesNotCompile(
      """val b: Baz.Baz1.type = Baz.Baz1
        |fooVar.signal.splitMatchOne(_.handleValue(b) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )

    // --- an inferred `val` (Scala infers `Tar.type`, but a `val` is still rejected) ---
    assertDoesNotCompile(
      """val x = Tar
        |fooVar.signal.splitMatchOne(_.handleValue(x) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )

    // --- a `var` ---
    assertDoesNotCompile(
      """var x: Foo = Tar
        |fooVar.signal.splitMatchOne(_.handleValue(x) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )

    // --- a method call (the silently-swallows-everything case the guard primarily protects) ---
    assertDoesNotCompile(
      """def mk(): Foo = Tar
        |fooVar.signal.splitMatchOne(_.handleValue(mk()) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )
    assertDoesNotCompile(
      """def mk(): Baz = Baz.Baz1
        |fooVar.signal.splitMatchOne(_.handleValue(mk()) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )

    // --- a (non-singleton) case class instance ---
    assertDoesNotCompile(
      """fooVar.signal.splitMatchOne(_.handleValue(Bar("x")) { Tar }, _.handleRest { _ => Tar })"""
    )
    // ...matching the Bar shape is what handleCase is for:
    assertCompiles(
      """fooVar.signal.splitMatchOne(_.handleCase { case Bar("x") => "x" } { _ => Tar }, _.handleRest { _ => Tar })"""
    )

    // --- a bare literal (no singleton type inferred) ---
    assertDoesNotCompile(
      """anyVar.signal.splitMatchOne(_.handleValue(42) { 0 }, _.handleRest { _ => 0 })"""
    )
    assertCompiles(
      """anyVar.signal.splitMatchOne(_.handleCase { case 42 => 0 } { _ => 0 }, _.handleRest { _ => 0 })"""
    )
  }

  it("rejects non-singletons (splitMatchSeq)") {
    assertDoesNotCompile(
      """val x: Foo = Tar
        |fooListVar.signal.splitMatchSeq(_.toString)(_.handleValue(x) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )
    assertDoesNotCompile(
      """def mk(): Baz = Baz.Baz1
        |fooListVar.signal.splitMatchSeq(_.toString)(_.handleValue(mk()) { Tar }, _.handleRest { _ => Tar })""".stripMargin
    )
    // The literal singleton still compiles in the Seq macro:
    assertCompiles(
      """fooListVar.signal.splitMatchSeq(_.toString)(_.handleValue(Tar) { Tar }, _.handleRest { _ => Tar })"""
    )
  }
}
