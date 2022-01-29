package com.raquo.airstream.timing

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class SyncDelayEventStreamSpec extends UnitSpec {

  it("emits after designated stream") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val var1 = Var(1)
    val var2 = Var(100)

    val stream1 = var1.signal.changes.map(Calculation.log("stream1", calculations))
    val stream2 = var2.signal.changes.map(Calculation.log("stream2", calculations))
    val stream1synced = stream1.delaySync(after = stream2).map(Calculation.log("stream1-synced", calculations))

    // --

    stream1synced.addObserver(Observer(effects += Effect("stream1-synced-obs", _)))
    stream2.addObserver(Observer(effects += Effect("stream2-obs", _)))

    // We're testing the order of calculations / effects achieved by delaySync

    Var.set(
      var1 -> 2,
      var2 -> 200
    )

    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 2),
      Calculation("stream2", 200),
      Calculation("stream1-synced", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("stream2-obs", 200),
      Effect("stream1-synced-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    Var.set(
      var1 -> 3,
      var2 -> 300
    )

    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 3),
      Calculation("stream2", 300),
      Calculation("stream1-synced", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("stream2-obs", 300),
      Effect("stream1-synced-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    // We don't require stream2 to emit in order for stream1 to emit

    var1.set(4)

    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 4),
      Calculation("stream1-synced", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("stream1-synced-obs", 4)
    )

    calculations.clear()
    effects.clear()

    // --

    var2.set(400)

    calculations shouldBe mutable.Buffer(
      Calculation("stream2", 400)
    )
    effects shouldBe mutable.Buffer(
      Effect("stream2-obs", 400)
    )

    calculations.clear()
    effects.clear()
  }

  it("multi-level, nested, dependent sync observables") {

    // See https://gitter.im/Laminar_/Lobby?at=6007655b36db01248a8bf5a9 and below

    implicit val testOwner: TestableOwner = new TestableOwner

    val v = Var(0)

    val calculations = mutable.Buffer[Calculation[Int]]()

    val signal1 = v.signal
      .map(Calculation.log("signal1", calculations))

    val signal2 = signal1
      .map(ev => ev)
      .map(Calculation.log("signal2", calculations))

    val signal3 = signal2
      .composeChanges { signal2Changes =>
        signal2Changes
          .delaySync(signal1.changes)
          .map(Calculation.log("signal3-changes-source", calculations))
      }
      .map(Calculation.log("signal3", calculations))

    val signal4 = signal1
      .composeChanges { signal1Changes =>
        signal1Changes
          .delaySync(signal3.changes)
          .map(Calculation.log("signal4-changes-source", calculations))
      }
      .map(Calculation.log("signal4", calculations))

    val signal5 = signal2
      .composeChanges { signal2Changes =>
        signal2Changes
          .delaySync(signal3.changes)
          .map(Calculation.log("signal5-changes-source", calculations))
      }
      .map(Calculation.log("signal5", calculations))

    // --

    // Order is important
    signal5.addObserver(Observer.empty)
    signal4.addObserver(Observer.empty)
    signal3.addObserver(Observer.empty)

    // signal4's and signal5's initial value does not depend on signal3's initial value,
    // only on its changes, so it's ok that signal3's initial value is evaluated later.
    assert(calculations.toList == List(
      Calculation("signal1", 0),
      Calculation("signal2", 0),
      Calculation("signal5", 0),
      Calculation("signal4", 0),
      Calculation("signal3", 0)
    ))

    calculations.clear()

    // --

    v.set(1)

    assert(calculations.toList == List(
      Calculation("signal1", 1),
      Calculation("signal2", 1),
      Calculation("signal3-changes-source", 1),
      Calculation("signal3", 1),
      Calculation("signal5-changes-source", 1),
      Calculation("signal5", 1),
      Calculation("signal4-changes-source", 1),
      Calculation("signal4", 1)
    ))

    calculations.clear()

    // --

    v.set(2)

    assert(calculations.toList == List(
      Calculation("signal1", 2),
      Calculation("signal2", 2),
      Calculation("signal3-changes-source", 2),
      Calculation("signal3", 2),
      Calculation("signal5-changes-source", 2),
      Calculation("signal5", 2),
      Calculation("signal4-changes-source", 2),
      Calculation("signal4", 2)
    ))

    calculations.clear()

  }
}
