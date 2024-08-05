package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}

import scala.collection.mutable
import scala.util.Success

class SignalSpec extends UnitSpec {

  it("EventStream.toSignal creates a properly wired Signal") {

    // @TODO add another mapped dependency on signal and verify that one's evaluations as well (maybe in a separate test?)

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val signalObserver1 = Observer[Int](effects += Effect("signal-obs-1", _))
    val signalObserver2 = Observer[Int](effects += Effect("signal-obs-2", _))

    val bus = new EventBus[Int]
    val signal = bus.events
      .map(Calculation.log("bus", calculations))
      .map(_ * 10)
      .toSignal(initial = -1)
      .map(Calculation.log("map-signal", calculations))

    // --

    // Signals are lazy: without an observer, nothing will happen
    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    // When observer is added, it immediately gets the last evaluated current value
    // Note: Because signal had no observer when bus fired value `1`, that event was NOT used to compute new value.
    val sub1 = signal.addObserver(signalObserver1)

    calculations shouldBe mutable.Buffer(
      Calculation(
        "map-signal",
        -1
      ) // First time current value of this signal was needed, so it is now calculated
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-1", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 2),
      Calculation("map-signal", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-1", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 2),
      Calculation("map-signal", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-1", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    // When adding a new observer, it gets the signal's current value.
    // Here the current value has been updated by the previous event, and the signal remembers it.
    val sub2 = signal.addObserver(signalObserver2)

    calculations shouldBe mutable.Buffer() // Using cached calculation
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-2", 20)
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 3),
      Calculation("map-signal", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-1", 30),
      Effect("signal-obs-2", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    bus.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 4),
      Calculation("map-signal", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-2", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    sub2.kill()

    bus.writer.onNext(5)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    signal.addObserver(signalObserver2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-2", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(6)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 6),
      Calculation("map-signal", 60)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs-2", 60)
    )

    calculations.clear()
    effects.clear()
  }

  it("Signal.changes lazily reflects the changes of underlying signal") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val signalObserver = Observer[Int](effects += Effect("signal-obs", _))
    val changesObserver = Observer[Int](effects += Effect("changes-obs", _))

    val bus = new EventBus[Int]
    val signal = bus.events
      .map(Calculation.log("bus", calculations))
      .map(_ * 10)
      .toSignal(initial = -1)
      .map(Calculation.log("map-signal", calculations))
    val changes = signal.changes.map(Calculation.log("changes", calculations))

    // .changes can't be a lazy val for memory management purposes
    // (parent should not have a reference to a child that has no observers)
    // #TODO is this actually a legit concern?
    //  - This simply links the two observables together for GC purposes
    //  - GC should still be able to eliminate them when both of them are
    //    no longer referenced
    //  - If .changes is a def, in some cases we could GC it sooner
    //    than if it was linked, but on the flip side, now we need to create
    //    a new observable for every consumer of .changes, instead of reusing it.
    signal.changes shouldNotBe signal.changes

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val subChanges1 = changes.addObserver(changesObserver)

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("map-signal", -1),
      Calculation("bus", 2),
      Calculation("map-signal", 20),
      Calculation("changes", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("changes-obs", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    // Adding observer to signal sends the last evaluated current value to it
    val subSignal = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 20)
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 3),
      Calculation("map-signal", 30),
      Calculation("changes", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 30),
      Effect("changes-obs", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 3),
      Calculation("map-signal", 30),
      Calculation("changes", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 30),
      Effect("changes-obs", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    subChanges1.kill()

    bus.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 4),
      Calculation("map-signal", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    // The changes stream missed a signal update (detected using parent.lastUpdateId),
    // so when it's restarted, it emits the parent's new current value.

    val subChanges2 = changes.addObserver(changesObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("changes", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("changes-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    subChanges2.kill()

    bus.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 4),
      Calculation("map-signal", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    // Same syncing behaviour, even when the signal emits the exact same value.
    // This is because we KNOW that the signal emitted by looking at lastUpdateId,
    // we don't approximate it with any kind of `nextValue == prevValue` checks.

    val subChanges3 = changes.addObserver(changesObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("changes", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("changes-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    subChanges3.kill()

    // Meanwhile if we the parent signal does not emit any updates while the
    // changes stream is stopped, the changes stream does not re-emit the
    // parent's current value when re-starting.

    val subChanges4 = changes.addObserver(changesObserver)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    subChanges4.kill()
    subSignal.kill()

    bus.writer.onNext(5)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
  }

  it("MapSignal.now/onNext re-evaluates project/initialValue when restarting") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val signalObserver = Observer[Int](effects += Effect("signal-obs", _))

    val bus = new EventBus[Int]
    val signal = bus.events
      .map(Calculation.log("bus", calculations))
      .map(_ * 10)
      .toSignal(initial = -1)
      .map(Calculation.log("map-signal", calculations))

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    signal.tryNow() shouldBe Success(-1)

    calculations shouldBe mutable.Buffer(
      Calculation("map-signal", -1)
    )
    effects shouldBe mutable.Buffer()

    calculations.clear()

    // --

    signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("map-signal", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 2),
      Calculation("map-signal", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    signal.tryNow()

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    signal.addObserver(Observer[Int](effects += Effect("signal-obs2", _)))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs2", 20)
    )

    effects.clear()

    // --

    signal.tryNow()

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
  }

}
