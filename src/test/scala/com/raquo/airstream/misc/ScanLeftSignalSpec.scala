package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class ScanLeftSignalSpec extends UnitSpec {

  it("ScanLeftSignal made with EventStream.scanLeft") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer[String](effects += Effect("signal-obs", _))

    val bus = new EventBus[Int]

    val signal = bus.events
      .scanLeft(initial = "numbers:"){ (acc, nextValue) => acc + " " + nextValue.toString }
      .map(Calculation.log("signal", calculations))

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers:")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers:")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 2")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 2")
    )

    calculations.clear()
    effects.clear()

    // --

    sub.kill()
    bus.writer.onNext(3)

    signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 2")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 2 4")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 2 4")
    )

    calculations.clear()
    effects.clear()

  }

  it("ScanLeftSignal made with Signal.scanLeft") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer[String](effects += Effect("signal-obs", _))

    val $var = Var(0)

    val signal = $var.signal
      .scanLeft(makeInitial = initial => s"numbers: init=${initial}"){ (acc, nextValue) => acc + " " + nextValue.toString }
      .map(Calculation.log("signal", calculations))

    $var.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub1 = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1")
    )

    calculations.clear()
    effects.clear()

    // --

    $var.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2")
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    $var.writer.onNext(3)

    val sub2 = signal.addObserver(signalObserver)

    // Re-synced to upstream
    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3")
    )

    calculations.clear()
    effects.clear()

    // --

    $var.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3 4")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4")
    )

    calculations.clear()
    effects.clear()

    // --

    sub2.kill()

    val sub3 = signal.addObserver(signalObserver)

    // If $var does not emit while this signal is stopped, we don't need to re-sync with it.
    calculations.shouldBeEmpty
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4")
    )

    effects.clear()

    // --

    sub3.kill()

    $var.writer.onNext(4)

    signal.addObserver(signalObserver)

    // We detect that $var has emitted an event while this signal was stopped, and get $var's current value
    // We don't care that 4 == 4, we KNOW that $var has emitted by looking at lastUpdateId internally.
    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3 4 4")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4 4")
    )

    calculations.clear()
    effects.clear()
  }
}
