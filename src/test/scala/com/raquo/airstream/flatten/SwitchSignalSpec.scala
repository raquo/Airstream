package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observable.MetaObservable
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

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

  it("Signal: emitting the same inner signal does not cause it to stop and re-start") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[String]]()

    // It's important that we reuse the exact same references to inner signals to check the logic
    // - fromSeq streams are used to ensure that onStart isn't called extraneously
    // - bus.events streams are used to ensure that onStop isn't called extraneously

    val outerBus = new EventBus[Int]

    val smallBus = new EventBus[String]

    val bigBus = new EventBus[String]

    val smallSignal = EventStream.merge(
      smallBus.events,
      EventStream.fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true)
    ).startWith("small-0")

    val bigSignal = EventStream.merge(
      bigBus.events,
      EventStream.fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true)
    ).startWith("big-0")

    val flatSignal = outerBus.events.startWith(0).flatMap {
      case i if i >= 10 => bigSignal
      case _ => smallSignal
    }.map(Calculation.log("flat", calculations))

    // --

    flatSignal.addObserver(Observer.empty)

    assert(calculations.toList == List(
      Calculation("flat", "small-0"),
      Calculation("flat", "small-1"),
      Calculation("flat", "small-2"),
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-0")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-0")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(1)

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-0")
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(2)

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-1")
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(10) // #Note switch to big

    assert(calculations.toList == List(
      Calculation("flat", "big-0"),
      Calculation("flat", "big-1"),
      Calculation("flat", "big-2")
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small bus - unrelated change")

    assert(calculations.isEmpty)

    // --

    bigBus.writer.onNext("big-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(11)

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-1")
    ))

    calculations.clear()

    // --

    bigBus.writer.onNext("big-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(5) // #Note switch back to small

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2") // Restore current value of small signal
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-3")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-3")
    ))

    calculations.clear()

    // --

    bigBus.writer.onNext("big bus - unrelated change")

    assert(calculations.isEmpty)
  }
}
