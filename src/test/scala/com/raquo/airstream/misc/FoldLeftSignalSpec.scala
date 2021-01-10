package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}

import scala.collection.mutable

class FoldLeftSignalSpec extends UnitSpec {

  // @TODO[Test] Verify Signal.fold and State.fold as well

  it("FoldSignal made with EventStream.fold") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer[String](effects += Effect("signal-obs", _))

    val bus = new EventBus[Int]

    val signal = bus.events
      .foldLeft(initial = "numbers:"){ (acc, nextValue) => acc + " " + nextValue.toString }
      .map(Calculation.log("signal", calculations))

    bus.writer.onNext(1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    val sub = signal.addObserver(signalObserver)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers:")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers:")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: 2")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: 2")
    )

    calculations.clear()
    effects.clear()

    // --

    sub.kill()
    bus.writer.onNext(3)

    signal.addObserver(signalObserver)
    bus.writer.onNext(4)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: 2 4")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: 2"), // new observer getting initial value
      Effect("signal-obs", "numbers: 2 4")
    )

    calculations.clear()
    effects.clear()

  }
}
