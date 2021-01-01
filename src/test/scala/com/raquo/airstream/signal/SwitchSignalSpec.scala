package com.raquo.airstream.signal

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observable.MetaObservable
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}

import scala.collection.mutable

class SwitchSignalSpec extends UnitSpec {

  it("mirrors last emitted signal, but only if subscribed") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    // Create 4 test vars and add logging to their streams
    val sourceVars = (1 to 4).map(_ => Var(-1))
    val sourceSignals = sourceVars.zipWithIndex.map {
      case (vr, index) => vr.signal.map(Calculation.log(s"source-$index", calculations))
    }

    val metaVar = Var(sourceSignals(0))

    val $latestNumber = metaVar.signal.flatten // SwitchSignalStrategy is the default (provided implicitly)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenSignal = $latestNumber.map(Calculation.log("flattened", calculations))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    val subFlatten = flattenSignal.addObserver(flattenObserver)

    calculations shouldEqual mutable.Buffer(
      Calculation("source-0", -1),
      Calculation("flattened", -1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(0).writer.onNext(0)

    calculations shouldEqual mutable.Buffer(
      Calculation("source-0", 0),
      Calculation("flattened", 0)
    )
    effects shouldEqual mutable.Buffer(
      Effect("flattened-obs", 0)
    )

    calculations.clear()
    effects.clear()

    // --

    metaVar.writer.onNext(sourceSignals(1))

    calculations shouldEqual mutable.Buffer(
      Calculation("source-1", -1),
      Calculation("flattened", -1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(1).writer.onNext(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("source-1", 1),
      Calculation("flattened", 1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("flattened-obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    metaVar.writer.onNext(sourceSignals(2))

    val source2Observer = Observer[Int](effects += Effect("source-2-obs", _))

    val source2Sub = sourceSignals(2).addObserver(source2Observer)
    subFlatten.kill()

    calculations shouldEqual mutable.Buffer(
      Calculation("source-2", -1),
      Calculation("flattened", -1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("flattened-obs", -1),
      Effect("source-2-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(2).writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("source-2", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("source-2-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    // flattened signal remembers its last tracked signal but wasn't keeping track of state so it emits old state

    flattenSignal.addObserver(flattenObserver) // re-activate flattened signal

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    effects.clear()

    // --

    sourceVars(2).writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("source-2", 3),
      Calculation("flattened", 3)
    )
    effects shouldEqual mutable.Buffer(
      Effect("source-2-obs", 3),
      Effect("flattened-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    source2Sub.kill()

    sourceVars(2).writer.onNext(4)

    calculations shouldEqual mutable.Buffer(
      Calculation("source-2", 4),
      Calculation("flattened", 4)
    )
    effects shouldEqual mutable.Buffer(
      Effect("flattened-obs", 4)
    )

    calculations.clear()
    effects.clear()
  }

}
