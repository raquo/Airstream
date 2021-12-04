package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}

import scala.collection.mutable

/** See also – diamond test case for this in GlitchSpec */
class SampleCombineEventStream2Spec extends UnitSpec {

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

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    val subCombined = combinedStream.addObserver(combinedObserver)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 0)
    )
    effects shouldEqual mutable.Buffer()

    calculations.clear()

    // --

    bus1.writer.onNext(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 1),
      Calculation("combined", 1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("combined", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus2.writer.onNext(100)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 100)
    )
    effects shouldEqual mutable.Buffer()

    calculations.clear()

    // --

    bus1.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 2),
      Calculation("combined", 102)
    )
    effects shouldEqual mutable.Buffer(
      Effect("combined", 102)
    )

    calculations.clear()
    effects.clear()

    // --

    subCombined.kill()
    sampledSignal.addObserver(signalObserver)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 100)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal", 100)
    )

    calculations.clear()
    effects.clear()

    // --

    bus2.writer.onNext(200)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 200)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal", 200)
    )

    calculations.clear()
    effects.clear()

    // --

    combinedStream.addObserver(combinedObserver)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    bus1.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 3),
      Calculation("combined", 203)
    )
    effects shouldEqual mutable.Buffer(
      Effect("combined", 203)
    )

    calculations.clear()
    effects.clear()
  }
}
