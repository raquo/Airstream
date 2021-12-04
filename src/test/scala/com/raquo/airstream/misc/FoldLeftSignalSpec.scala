package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class FoldLeftSignalSpec extends UnitSpec {

  it("FoldSignal made with EventStream.foldLeft") {

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

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: 2")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: 2")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(4)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: 2 4")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: 2 4")
    )

    calculations.clear()
    effects.clear()

  }

  it("FoldSignal made with Signal.foldLeft") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer[String](effects += Effect("signal-obs", _))

    val $var = Var(0)

    val signal = $var.signal
      .foldLeft(makeInitial = initial => s"numbers: init=${initial}"){ (acc, nextValue) => acc + " " + nextValue.toString }
      .map(Calculation.log("signal", calculations))

    $var.writer.onNext(1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    val sub1 = signal.addObserver(signalObserver)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: init=1")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: init=1")
    )

    calculations.clear()
    effects.clear()

    // --

    $var.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: init=1 2")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2")
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    $var.writer.onNext(3)

    val sub2 = signal.addObserver(signalObserver)

    // Re-synced to upstream
    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3")
    )

    calculations.clear()
    effects.clear()

    // --

    $var.writer.onNext(4)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3 4")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4")
    )

    calculations.clear()
    effects.clear()

    // --

    sub2.kill()

    $var.writer.onNext(4)

    signal.addObserver(signalObserver)

    // Re-synced to upstream without emitting an extraneous `4`
    calculations shouldEqual mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3 4")
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4")
    )

    calculations.clear()
    effects.clear()
  }
}
