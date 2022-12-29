package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observable.MetaObservable
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable


class SwitchSignalStreamSpec extends UnitSpec {

  it("mirrors last emitted signal, but only if subscribed") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    // Create 4 test vars and add logging to their streams
    val sourceVars = (1 to 4).map(_ => Var(-1))
    val sourceSignals = sourceVars.zipWithIndex.map {
      case (vr, index) => vr.signal.map(Calculation.log(s"signal-$index", calculations))
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
      Calculation("signal-0", -1),
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
      Calculation("signal-0", 0),
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
      Calculation("signal-1", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // -- re-emit the same signal

    metaBus.writer.onNext(sourceSignals(1))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceVars(1).writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-1", 1),
      Calculation("flattened", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    metaBus.writer.onNext(sourceSignals(2))

    val source2Observer = Observer[Int](effects += Effect("signal-2-obs", _))

    val source2Sub = sourceSignals(2).addObserver(source2Observer)
    subFlatten.kill()

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1),
      Effect("signal-2-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(2).writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-2-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    // This stream fires an event with the current signal's current value when restarting,
    // IF the signal has emitted while the stream was stopped. This is similar to the new
    // signal.changes restart logic.

    flattenStream.addObserver(flattenObserver) // re-activate flattened signal

    calculations shouldBe mutable.Buffer(
      Calculation("flattened", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 2)
    )
    calculations.clear()
    effects.clear()

    // --

    sourceVars(2).writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", 3),
      Calculation("flattened", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-2-obs", 3),
      Effect("flattened-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    source2Sub.kill()

    sourceVars(2).writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", 4),
      Calculation("flattened", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 4)
    )

    calculations.clear()
    effects.clear()
  }

  it("start & restart event order") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val manualBus = new EventBus[Int]

    // Create 4 test vars and add logging to their streams
    val sourceStreams = (1 to 4).map(n => EventStream.merge(manualBus.events, EventStream.fromSeq(List(1, 2).map(n * 10 + _))))
    val sourceSignals = sourceStreams.zipWithIndex.map {
      case (stream, index) => stream.startWith(0).map(Calculation.log(s"signal-$index", calculations))
    }

    val metaBus = new EventBus[Signal[Int]]

    val $latestNumber = metaBus.events.flatten // SwitchSignalStreamStrategy is the default (provided implicitly)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenStream = $latestNumber.map(Calculation.log("flattened", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub1 = flattenStream.addObserver(flattenObserver)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    val source0 = sourceSignals(0)

    metaBus.emit(source0)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0",0),
      Calculation("flattened",0),
      Calculation("signal-0",11),
      Calculation("flattened",11),
      Calculation("signal-0",12),
      Calculation("flattened",12)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 0),
      Effect("flattened-obs", 11),
      Effect("flattened-obs", 12)
    )
    calculations.clear()
    effects.clear()

    // -- EventStream.fromSeq re-emits values on restart

    sub1.kill()

    val sub2 = flattenStream.addObserver(flattenObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0", 11),
      Calculation("flattened", 11),
      Calculation("signal-0", 12),
      Calculation("flattened", 12)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 11),
      Effect("flattened-obs", 12)
    )
    calculations.clear()
    effects.clear()

    // --

    val tempSub = source0.addObserver(Observer.empty)(owner)

    sub2.kill()

    manualBus.emit(10)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0", 10)
    )
    effects shouldBe mutable.Buffer()

    calculations.clear()

    // -- when restarting, the flattened stream should emit the current value (10)
    //    first, followed by 11 and 12.

    tempSub.kill()

    val sub3 = flattenStream.addObserver(flattenObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("flattened", 10),
      Calculation("signal-0", 11),
      Calculation("flattened", 11),
      Calculation("signal-0", 12),
      Calculation("flattened", 12)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 10),
      Effect("flattened-obs", 11),
      Effect("flattened-obs", 12)
    )
    calculations.clear()
    effects.clear()

    // --

    metaBus.emit(sourceSignals(1))

    calculations shouldBe mutable.Buffer(
      Calculation("signal-1", 0),
      Calculation("flattened", 0),
      Calculation("signal-1", 21),
      Calculation("flattened", 21),
      Calculation("signal-1", 22),
      Calculation("flattened", 22)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 0),
      Effect("flattened-obs", 21),
      Effect("flattened-obs", 22)
    )
    calculations.clear()
    effects.clear()

    // --

    manualBus.emit(20)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-1", 20),
      Calculation("flattened", 20),
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 20)
    )
    calculations.clear()
    effects.clear()

  }
}
