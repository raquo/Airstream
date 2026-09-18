package splitmatch.publicapi

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

/** Regression tests for the public contract of `splitMatchSeq` handler signals.
  *
  * IMPORTANT: this spec deliberately lives OUTSIDE the `com.raquo.airstream` package.
  * `Signal.now()` is `protected[airstream]`, so `SplitMatchSeqSpec` – which sits inside that
  * package – can call `.now()` on a plain `Signal` and compile. A real user cannot. Any test of
  * what users can do with handler signals has to be written from where users are.
  *
  * The `splitMatchSeq` docstring promises: "The signals provided in the callbacks are
  * StrictSignal-s, so you can read .now() from them." The runtime child signals ARE StrictSignals
  * (`splitSeq` gives a `KeyedStrictSignal`, and `splitMatchOne` handlers already receive a
  * `StrictSignal`), so the seq handlers must be typed as `StrictSignal` for that promise to hold.
  *
  * `assertCompiles` type-checks the snippet in THIS file's scope, so a failure here shows up as
  * a failed test rather than as a broken test build.
  */
class SplitMatchSeqStrictSignalRegressionSpec extends UnitSpec {

  sealed trait Foo {
    def id: String
  }
  final case class FooC(id: String, version: Int) extends Foo
  case object FooO extends Foo {
    override val id: String = "object"
  }

  final case class Res(result: String)

  val bus: EventBus[List[Foo]] = new EventBus[List[Foo]]
  val listVar: Var[List[Foo]] = Var[List[Foo]](Nil)

  it("handleCase: handler signal is a StrictSignal, so users can read .now() from it") {
    assertCompiles(
      """bus.events.splitMatchSeq(_.id)(
        |  _.handleCase { case FooC(id, version) => version } { versionSignal => Res(versionSignal.now().toString) },
        |  _.handleRest { _ => Res("rest") },
        |)""".stripMargin
    )
  }

  it("handleType: handler signal is a StrictSignal, so users can read .now() from it") {
    assertCompiles(
      """bus.events.splitMatchSeq(_.id)(
        |  _.handleType[FooC] { fooCSignal => Res(fooCSignal.now().id) },
        |  _.handleRest { _ => Res("rest") },
        |)""".stripMargin
    )
  }

  it("handleRest: handler signal is a StrictSignal, so users can read .now() from it") {
    assertCompiles(
      """bus.events.splitMatchSeq(_.id)(
        |  _.handleValue(FooO) { Res("object") },
        |  _.handleRest { fooSignal => Res(fooSignal.now().id) },
        |)""".stripMargin
    )
  }

  it("runtime: users can read .now() from seq handler signals and it reflects the element") {
    val effects = mutable.Buffer[Effect[String]]()
    val owner = new TestableOwner

    val signal: Signal[List[Res]] = listVar.signal.splitMatchSeq(_.id)(
      _.handleCase { case FooC(id, version) => version } { versionSignal =>
        effects += Effect("init-child", s"v${versionSignal.now()}")
        Res(s"c-${versionSignal.now()}")
      },
      _.handleRest { fooSignal =>
        effects += Effect("init-rest", fooSignal.now().id)
        Res(fooSignal.now().id)
      },
    )

    signal.foreach { res =>
      effects += Effect("result", res.map(_.result).mkString(","))
    }(owner)

    effects shouldBe mutable.Buffer(Effect("result", ""))
    effects.clear()

    listVar.set(FooC("a", 1) :: FooO :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "v1"),
      Effect("init-rest", "object"),
      Effect("result", "c-1,object")
    )
  }

  it("a handler written against the wider Signal type still compiles (source compatibility)") {
    // Function1 is contravariant in its parameter, so a `Signal[B] => O` handler is a valid
    // `StrictSignal[B] => O`. Existing user code that ascribes `Signal` must keep working.
    assertCompiles(
      """bus.events.splitMatchSeq(_.id)(
        |  _.handleType[FooC] { (fooCSignal: Signal[FooC]) => Res("c") },
        |  _.handleRest { (fooSignal: Signal[Foo]) => Res("rest") },
        |)""".stripMargin
    )
  }
}
