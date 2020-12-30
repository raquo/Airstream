package com.raquo.airstream.signal

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}

import scala.collection.mutable

/** See also – diamond test case for this in GlitchSpec */
class SampleCombineSignal2Spec extends UnitSpec {

  it("gets current value of Signal") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val samplingVar = Var(100)
    val sampledVar = Var(10)

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val sampledSignal = sampledVar.signal.map(Calculation.log("sampled", calculations))

    val combinedSignal = samplingVar.signal
      .map(Calculation.log("sampling", calculations))
      .withCurrentValueOf(sampledSignal)
      .map2(_ + _)
      .map(Calculation.log("combined", calculations))

    val sampledObserver = Observer[Int](effects += Effect("sampled", _))
    val combinedObserver = Observer[Int](effects += Effect("combined", _))

    // --

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    val subCombined = combinedSignal.addObserver(combinedObserver)

    calculations shouldEqual mutable.Buffer(
      Calculation("sampling", 100),
      Calculation("sampled", 10),
      Calculation("combined", 110),
    )
    effects shouldEqual mutable.Buffer(
      Effect("combined", 110),
    )

    calculations.clear()
    effects.clear()

    // --

    samplingVar.writer.onNext(200)

    calculations shouldEqual mutable.Buffer(
      Calculation("sampling", 200),
      Calculation("combined", 210)
    )
    effects shouldEqual mutable.Buffer(
      Effect("combined", 210)
    )

    calculations.clear()
    effects.clear()

    // --

    sampledVar.writer.onNext(20)

    calculations shouldEqual mutable.Buffer(
      Calculation("sampled", 20)
    )
    effects shouldEqual mutable.Buffer()

    calculations.clear()

    // --

    samplingVar.writer.onNext(300)

    calculations shouldEqual mutable.Buffer(
      Calculation("sampling", 300),
      Calculation("combined", 320)
    )
    effects shouldEqual mutable.Buffer(
      Effect("combined", 320)
    )

    calculations.clear()
    effects.clear()

    // --

    subCombined.kill()
    sampledSignal.addObserver(sampledObserver)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("sampled", 20)
    )

    effects.clear()

    // --

    sampledVar.writer.onNext(30)

    calculations shouldEqual mutable.Buffer(
      Calculation("sampled", 30)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sampled", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    combinedSignal.addObserver(combinedObserver)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("combined", 320) // @TODO[API] This could be 330 if we implement https://github.com/raquo/Airstream/issues/43
    )

    effects.clear()

    // --

    samplingVar.writer.onNext(400)

    calculations shouldEqual mutable.Buffer(
      Calculation("sampling", 400),
      Calculation("combined", 430)
    )
    effects shouldEqual mutable.Buffer(
      Effect("combined", 430)
    )

    calculations.clear()
    effects.clear()
  }
}
