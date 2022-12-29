package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}

import scala.collection.mutable

/** See also – diamond test case for this in GlitchSpec */
class SampleCombineStream2Spec extends UnitSpec {

  it("gets current value of Signal") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Int]

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val sampledSignal = bus2.events.startWith(0).map(Calculation.log("signal", calculations))

    val combinedStream = bus1.events
      .map(Calculation.log("stream", calculations))
      .withCurrentValueOf(sampledSignal)
      .mapN(_ + _)
      .map(Calculation.log("combined", calculations))

    val signalObserver = Observer[Int](effects += Effect("signal", _))
    val combinedObserver = Observer[Int](effects += Effect("combined", _))

    // --

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val subCombined = combinedStream.addObserver(combinedObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 0)
    )
    effects shouldBe mutable.Buffer()

    calculations.clear()

    // --

    bus1.writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 1),
      Calculation("combined", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("combined", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus2.writer.onNext(100)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 100)
    )
    effects shouldBe mutable.Buffer()

    calculations.clear()

    // --

    bus1.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 2),
      Calculation("combined", 102)
    )
    effects shouldBe mutable.Buffer(
      Effect("combined", 102)
    )

    calculations.clear()
    effects.clear()

    // --

    subCombined.kill()
    sampledSignal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal", 100)
    )

    effects.clear()

    // --

    bus2.writer.onNext(200)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 200)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal", 200)
    )

    calculations.clear()
    effects.clear()

    // --

    combinedStream.addObserver(combinedObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    bus1.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 3),
      Calculation("combined", 203)
    )
    effects shouldBe mutable.Buffer(
      Effect("combined", 203)
    )

    calculations.clear()
    effects.clear()
  }
}
