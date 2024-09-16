package com.raquo.airstream.split

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var
import com.raquo.airstream.split.SplittableTypeMacros.*

import scala.collection.{immutable, mutable}
import scala.scalajs.js
import com.raquo.airstream.ShouldSyntax.shouldBeEmpty

class SplittableTypeSpec extends UnitSpec {

  sealed trait Foo

  final case class Bar(strOpt: Option[String]) extends Foo
  enum Baz extends Foo {
    case Baz1, Baz2
  }
  case object Tar extends Foo

  final case class Res(result: Any)
  
  it("split match signal") {
    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[Foo](Bar(Some("initial")))

    val owner = new TestableOwner

    val signal = myVar.signal
      .splitMatch
      .handleCase {
        case Bar(Some(str)) => str
        case Bar(None) => "null"
      } { case (str, strSignal) =>
        effects += Effect("init-child", s"Bar-$str")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        strSignal.foreach { str =>
          effects += Effect("update-child", s"Bar-$str")
        }(owner)

        Res("Bar")
      }
      .handleType[Baz] { case (baz, bazSignal) =>
        effects += Effect("init-child", s"Baz-${baz.ordinal}-${baz.toString}")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        bazSignal.foreach { baz =>
          effects += Effect("update-child", s"Baz-${baz.ordinal}-${baz.toString}")
        }(owner)

        Res("Baz")
      }
      .handleCase { case Tar => 10 } { case (int, intSignal) =>
        effects += Effect("init-child", s"Tar-${int}")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        intSignal.foreach { int =>
          effects += Effect("update-child", s"Tar-${int}")
        }(owner)

        Res("Tar")
      }
      .toSignal
    
    signal.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Bar-initial"),
      Effect("update-child", "Bar-initial"),
      Effect("result", "Res(Bar)")
    )

    effects.clear()

    myVar.writer.onNext(Bar(None))

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Bar)"),
      Effect("update-child", "Bar-null")
    )

    effects.clear()

    myVar.writer.onNext(Bar(None))

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Bar)"), // sematically, splitMatch/handleCase/toSignal use splitOne underlying, so this is the same as splitOne spec
      Effect("update-child", "Bar-null")
    )

    effects.clear()

    myVar.writer.onNext(Bar(Some("other")))

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Bar)"),
      Effect("update-child", "Bar-other")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Baz.Baz1)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Baz-0-Baz1"),
      Effect("update-child", "Baz-0-Baz1"),
      Effect("result", "Res(Baz)")
    )

    effects.clear()

    myVar.writer.onNext(Baz.Baz2)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Baz)"),
      Effect("update-child", "Baz-1-Baz2")
    )

    effects.clear()

    myVar.writer.onNext(Baz.Baz2)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Baz)"),
      Effect("update-child", "Baz-1-Baz2")
    )

    effects.clear()

    myVar.writer.onNext(Tar)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Tar-10"),
      Effect("update-child", "Tar-10"),
      Effect("result", "Res(Tar)")
    )

    effects.clear()

    myVar.writer.onNext(Tar)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Tar)"),
      Effect("update-child", "Tar-10")
    )

    effects.clear()

  }

  it("split match signal - with warning in compiler") {
    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[Foo](Bar(Some("initial")))

    val owner = new TestableOwner

    // This should warn "match may not be exhaustive" with mising cases, and some idea can also flag it
    val signal = myVar.signal
      .splitMatch
      .handleCase {
        case Bar(Some(str)) => str
      } { case (str, strSignal) =>
        effects += Effect("init-child", s"Bar-$str")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        strSignal.foreach { str =>
          effects += Effect("update-child", s"Bar-$str")
        }(owner)

        Res("Bar")
      }
      .handleCase { case baz: Baz.Baz1.type => baz } { case (baz, bazSignal) =>
        effects += Effect("init-child", s"Baz-${baz.ordinal}-${baz.toString}")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        bazSignal.foreach { baz =>
          effects += Effect("update-child", s"Baz-${baz.ordinal}-${baz.toString}")
        }(owner)

        Res("Baz1")
      }
      .handleCase { case Tar => 10 } { case (int, intSignal) =>
        effects += Effect("init-child", s"Tar-${int}")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        intSignal.foreach { int =>
          effects += Effect("update-child", s"Tar-${int}")
        }(owner)

        Res("Tar")
      }
      .toSignal
    
    signal.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Bar-initial"),
      Effect("update-child", "Bar-initial"),
      Effect("result", "Res(Bar)")
    )

    effects.clear()

    myVar.writer.onNext(Bar(Some("other")))

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Bar)"),
      Effect("update-child", "Bar-other")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Baz.Baz1)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Baz-0-Baz1"),
      Effect("update-child", "Baz-0-Baz1"),
      Effect("result", "Res(Baz1)")
    )

    effects.clear()

    myVar.writer.onNext(Tar)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Tar-10"),
      Effect("update-child", "Tar-10"),
      Effect("result", "Res(Tar)")
    )

    effects.clear()

    myVar.writer.onNext(Tar)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Tar)"),
      Effect("update-child", "Tar-10")
    )

    effects.clear()
    
  }

  it("split match stream") {
    val effects = mutable.Buffer[Effect[String]]()

    val myEventBus = new EventBus[Foo]

    val owner = new TestableOwner

    val stream = myEventBus.events
      .splitMatch
      .handleCase {
        case Bar(Some(str)) => str
        case Bar(None) => "null"
      } { case (str, strSignal) =>
        effects += Effect("init-child", s"Bar-$str")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        strSignal.foreach { str =>
          effects += Effect("update-child", s"Bar-$str")
        }(owner)

        Res("Bar")
      }
      .handleType[Baz] { case (baz, bazSignal) =>
        effects += Effect("init-child", s"Baz-${baz.ordinal}-${baz.toString}")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        bazSignal.foreach { baz =>
          effects += Effect("update-child", s"Baz-${baz.ordinal}-${baz.toString}")
        }(owner)

        Res("Baz")
      }
      .handleCase { case Tar => 10 } { case (int, intSignal) =>
        effects += Effect("init-child", s"Tar-${int}")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        intSignal.foreach { int =>
          effects += Effect("update-child", s"Tar-${int}")
        }(owner)

        Res("Tar")
      }
      .toStream
    
    stream.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    myEventBus.writer.onNext(Bar(None))

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Bar-null"),
      Effect("update-child", "Bar-null"),
      Effect("result", "Res(Bar)")
    )

    effects.clear()

    myEventBus.writer.onNext(Bar(None))

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Bar)"), // sematically, splitMatch/handleCase/toStream use splitOne underlying, so this is the same as splitOne spec
      Effect("update-child", "Bar-null")
    )

    effects.clear()

    myEventBus.writer.onNext(Bar(Some("other")))

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Bar)"),
      Effect("update-child", "Bar-other")
    )

    effects.clear()

    // --

    myEventBus.writer.onNext(Baz.Baz1)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Baz-0-Baz1"),
      Effect("update-child", "Baz-0-Baz1"),
      Effect("result", "Res(Baz)")
    )

    effects.clear()

    myEventBus.writer.onNext(Baz.Baz2)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Baz)"),
      Effect("update-child", "Baz-1-Baz2")
    )

    effects.clear()

    myEventBus.writer.onNext(Baz.Baz2)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Baz)"),
      Effect("update-child", "Baz-1-Baz2")
    )

    effects.clear()

    myEventBus.writer.onNext(Tar)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Tar-10"),
      Effect("update-child", "Tar-10"),
      Effect("result", "Res(Tar)")
    )

    effects.clear()

    myEventBus.writer.onNext(Tar)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Tar)"),
      Effect("update-child", "Tar-10")
    )

    effects.clear()

  }

  it("split match stream - with warning in compiler") {
    val effects = mutable.Buffer[Effect[String]]()

    val myEventBus = new EventBus[Foo]

    val owner = new TestableOwner

    // This should warn "match may not be exhaustive" with mising cases, and some idea can also flag it
    // Compiler only flag the first warning in some case, so it's best to comment out first warning test for this to flag the warning
    val stream = myEventBus.events
      .splitMatch
      .handleCase {
        case Bar(None) => "null"
      } { case (str, strSignal) =>
        effects += Effect("init-child", s"Bar-$str")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        strSignal.foreach { str =>
          effects += Effect("update-child", s"Bar-$str")
        }(owner)

        Res("Bar")
      }
      .handleType[Baz] { case (baz, bazSignal) =>
        effects += Effect("init-child", s"Baz-${baz.ordinal}-${baz.toString}")
        // @Note keep foreach or addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        bazSignal.foreach { baz =>
          effects += Effect("update-child", s"Baz-${baz.ordinal}-${baz.toString}")
        }(owner)

        Res("Baz")
      }
      .toStream
    
    stream.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    myEventBus.writer.onNext(Bar(None))

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Bar-null"),
      Effect("update-child", "Bar-null"),
      Effect("result", "Res(Bar)")
    )

    effects.clear()

    myEventBus.writer.onNext(Bar(None))

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Bar)"), // sematically, splitMatch/handleCase/toStream use splitOne underlying, so this is the same as splitOne spec
      Effect("update-child", "Bar-null")
    )

    effects.clear()

    // --

    myEventBus.writer.onNext(Baz.Baz1)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "Baz-0-Baz1"),
      Effect("update-child", "Baz-0-Baz1"),
      Effect("result", "Res(Baz)")
    )

    effects.clear()

    myEventBus.writer.onNext(Baz.Baz2)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Baz)"),
      Effect("update-child", "Baz-1-Baz2")
    )

    effects.clear()

    myEventBus.writer.onNext(Baz.Baz2)

    effects shouldBe mutable.Buffer(
      Effect("result", "Res(Baz)"),
      Effect("update-child", "Baz-1-Baz2")
    )

    effects.clear()

  }

}
