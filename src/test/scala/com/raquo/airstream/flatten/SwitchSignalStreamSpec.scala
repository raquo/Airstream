package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observable.MetaObservable
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.{Val, Var}

import scala.collection.mutable


class SwitchSignalStreamSpec extends UnitSpec {

  it("mirrors last emitted signal, but only if subscribed") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    // Create 4 test vars and add logging to their streams
    val sourceVars = (1 to 4).map(_ => Var(-1))
    val sourceSignals = sourceVars.zipWithIndex.map {
      case (vr, index) => vr.signal.map(Calculation.log(s"source-$index", calculations))
    }

    val metaBus = new EventBus[Signal[Int]]

    val $latestNumber = metaBus.events.flatten // SwitchSignalStreamStrategy is the default (provided implicitly)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenStream = $latestNumber.map(Calculation.log("flattened", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val subFlatten = flattenStream.addObserver(flattenObserver)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    metaBus.emit(sourceSignals(0))

    calculations shouldBe mutable.Buffer(
      Calculation("source-0", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(0).writer.onNext(0)

    calculations shouldBe mutable.Buffer(
      Calculation("source-0", 0),
      Calculation("flattened", 0)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 0)
    )

    calculations.clear()
    effects.clear()

    // --

    metaBus.writer.onNext(sourceSignals(1))

    calculations shouldBe mutable.Buffer(
      Calculation("source-1", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(1).writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("source-1", 1),
      Calculation("flattened", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    metaBus.writer.onNext(sourceSignals(2))

    val source2Observer = Observer[Int](effects += Effect("source-2-obs", _))

    val source2Sub = sourceSignals(2).addObserver(source2Observer)
    subFlatten.kill()

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1),
      Effect("source-2-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(2).writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    // This stream behaves like any stream, does not re-emit the current signal's current value
    // This behaviour to similar to signal.changes which does not re-emit the signal's value
    // when it's subscribed to.

    flattenStream.addObserver(flattenObserver) // re-activate flattened signal

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    sourceVars(2).writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 3),
      Calculation("flattened", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 3),
      Effect("flattened-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    source2Sub.kill()

    sourceVars(2).writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 4),
      Calculation("flattened", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 4)
    )

    calculations.clear()
    effects.clear()
  }
}
