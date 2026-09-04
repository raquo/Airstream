package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.TestableOwner
import com.raquo.airstream.state.Var

import scala.collection.mutable

/** Regression reproduction for a spurious "unreachable case" compiler warning.
  *
  * When you pattern-match (via `.map` or `.collect`) on the tuple produced by
  * `withCurrentValueOf`, and the pattern destructures some of the tuple elements
  * with `Some(_)` / `None`, Scala 3 emits false-positive warnings:
  *
  *   - [E030] Match case Unreachable Warning  (on the real, reachable case)
  *   - [E121] Unreachable case except for null (on a trailing `case _`)
  *
  * Root cause: `withCurrentValueOf` returns `EventStream[c.Composed]`, where
  * `c: app.tulz.tuplez.Compose` computes `Composed` via a match type
  * (`Tuple.Concat[Tuple1[A], R]`). Because `c` is a method parameter that has
  * gone out of scope, the element type reaches the pattern match as an
  * existentially-skolemized abstract member `?1.Composed` backed by that match
  * type. The pattern-match reachability checker (SpaceEngine) does not reduce
  * this to a concrete `TupleN`, so it cannot see that the `Some(_)` cases are
  * matchable and flags them as unreachable.
  *
  * The old tuplez `Composition` typeclass produced concrete per-arity tuple
  * types (`type Composed = (T1, T2, T3, R)`), which the checker could
  * decompose, so this did not warn before.
  *
  * The warnings are false positives: the runtime values really are the expected
  * tuples, as the assertions below verify (this spec passes). Compiling this file
  * emits the warnings above; once the underlying issue is fixed it should compile
  * clean.
  *
  * Version-specific: reproduced on Scala 3.8.4 (`sbt "++3.8.4! airstream/Test/compile"`);
  * compiles clean on 3.3.8 LTS, so this is a newer-SpaceEngine regression rather
  * than an Airstream/tuplez logic error.
  *
  * Workaround: ascribe the concrete tuple type before matching, e.g.
  * {{{ .map(identity[(Int, String, Option[Int], Option[Long])]) }}}
  */
class WithCurrentValueOfPatternMatchSpec extends UnitSpec {

  it("`.map` with a Some/None pattern and a trailing `case _` (E030 + E121)") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus = new EventBus[Int]
    val preset = Var("preset")
    val maybeA = Var(Option("a"))
    val maybeB = Var(Option(1L))

    val results = mutable.Buffer[String]()

    val stream =
      bus.events
        .withCurrentValueOf(preset.signal, maybeA.signal, maybeB.signal)
        .map {
          case (n, p, Some(a), Some(b)) => s"$n-$p-$a-$b"
          case _                        => "none"
        }

    stream.addObserver(Observer(results += _))

    bus.writer.onNext(7)
    // The Some/Some case IS reachable, despite the "unreachable" warning:
    results shouldBe mutable.Buffer("7-preset-a-1")

    results.clear()

    maybeA.set(None)
    bus.writer.onNext(8)
    // ...and so is the `case _`, despite the "unreachable except null" warning:
    results shouldBe mutable.Buffer("none")
  }

  it("`.collect` with a Some pattern across a 5-tuple (E030)") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus = new EventBus[Int]
    val maybeX = Var(Option("x"))
    val maybeY = Var(Option("y"))
    val lup = Var("lup")
    val maybeZ = Var(Option("z"))

    val results = mutable.Buffer[(Int, String, String, String, String)]()

    val stream =
      bus.events
        .withCurrentValueOf(maybeX.signal, maybeY.signal, lup.signal, maybeZ.signal)
        .collect { case (ev, Some(x), Some(y), l, Some(z)) => (ev, x, y, l, z) }

    stream.addObserver(Observer(results += _))

    bus.writer.onNext(5)
    // The collected case IS reachable, despite the "unreachable" warning:
    results shouldBe mutable.Buffer((5, "x", "y", "lup", "z"))

    results.clear()

    maybeY.set(None)
    bus.writer.onNext(6)
    // Now the guard-like Some(y) fails, so nothing is collected:
    results shouldBe mutable.Buffer()
  }
}
