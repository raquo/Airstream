package com.raquo.airstream.split

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Signal
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

/** Pins down symbol ownership of `handleValue` handler bodies.
  *
  * `handleValue(v)(handler: => O)` takes its handler by-name. The macro wraps the user's
  * expression tree in a synthesized `(_: StrictSignal[V]) => handler` lambda – the one place
  * where user code is moved across a lambda boundary the user did not write. Local definitions
  * inside that block – a `val`, a `def`, a nested lambda – must end up owned by the synthesized
  * lambda, or the owner chain is inconsistent and LambdaLift (which decides captures by owner)
  * can miscompile.
  *
  * This currently holds (verified with `-Xcheck-macros`), but the other specs only ever put
  * plain expressions inside `handleValue { ... }`, so nothing exercised this shape. These tests
  * make sure a future change to how the handler is re-owned cannot regress silently.
  */
class SplitMatchHandleValueOwnerSpec extends UnitSpec {

  sealed trait Foo
  final case class Bar(str: String) extends Foo
  case object Tar extends Foo
  enum Baz extends Foo {
    case Baz1, Baz2
  }

  final case class Res(result: String)

  it("splitMatchOne: handleValue body may define a local val") {
    val effects = mutable.Buffer[Effect[String]]()
    val myVar = Var[Foo](Bar("initial"))
    val owner = new TestableOwner

    val signal: Signal[Res] = myVar.signal.splitMatchOne(
      _.handleType[Bar] { barSignal => Res(s"Bar-${barSignal.now().str}") },
      _.handleValue(Tar) {
        val label = "Tar"
        val count = effects.size
        effects += Effect("init-child", s"$label-$count")
        Res(label)
      },
      _.handleRest { _ => Res("Rest") },
    )

    signal.foreach { res =>
      effects += Effect("result", res.result)
    }(owner)

    effects shouldBe mutable.Buffer(Effect("result", "Bar-initial"))
    effects.clear()

    myVar.writer.onNext(Tar)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Tar-0"),
      Effect("result", "Tar")
    )
  }

  it("splitMatchOne: handleValue body may define a local def and a nested lambda") {
    val effects = mutable.Buffer[Effect[String]]()
    val myVar = Var[Foo](Tar)
    val owner = new TestableOwner

    val signal: Signal[Res] = myVar.signal.splitMatchOne(
      _.handleValue(Tar) {
        def render(n: Int): String = s"Tar-$n"
        val parts = List(1, 2, 3).map(n => render(n))
        effects += Effect("init-child", parts.mkString(","))
        Res("Tar")
      },
      _.handleValue(Baz.Baz1) {
        val nested = () => "Baz1"
        Res(nested())
      },
      _.handleRest { _ => Res("Rest") },
    )

    signal.foreach { res =>
      effects += Effect("result", res.result)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Tar-1,Tar-2,Tar-3"),
      Effect("result", "Tar")
    )
    effects.clear()

    myVar.writer.onNext(Baz.Baz1)

    effects shouldBe mutable.Buffer(Effect("result", "Baz1"))
  }

  it("splitMatchSeq: handleValue body may define a local val") {
    val effects = mutable.Buffer[Effect[String]]()
    val bus = new EventBus[List[Foo]]
    val owner = new TestableOwner

    val signal: Signal[List[Res]] = bus.events.splitMatchSeq(_.toString)(
      _.handleType[Bar] { barSignal => Res("Bar") },
      _.handleValue(Tar) {
        val label = "Tar"
        effects += Effect("init-child", label)
        Res(label)
      },
      _.handleRest { _ => Res("Rest") },
    )

    signal.foreach { res =>
      effects += Effect("result", res.map(_.result).mkString(","))
    }(owner)

    effects shouldBe mutable.Buffer(Effect("result", ""))
    effects.clear()

    bus.writer.onNext(Bar("a") :: Tar :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Tar"),
      Effect("result", "Bar,Tar")
    )
  }

  it("splitMatchOne: handleCase rhs may define a local val (control – already worked)") {
    val myVar = Var[Foo](Bar("initial"))
    val owner = new TestableOwner
    val results = mutable.Buffer[String]()

    val signal: Signal[Res] = myVar.signal.splitMatchOne(
      _.handleCase {
        case Bar(str) =>
          val upper = str.toUpperCase
          upper
      } { upperSignal => Res(upperSignal.now()) },
      _.handleRest { _ => Res("Rest") },
    )

    signal.foreach(res => results += res.result)(owner)

    results shouldBe mutable.Buffer("INITIAL")
  }
}
