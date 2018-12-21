package com.raquo.airstream.signal

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable
import scala.util.Success

class SignalSpec extends FunSpec with Matchers {

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

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    // When observer is added, it immediately gets the last evaluated current value
    // Note: Because signal had no observer when bus fired value `1`, that event was NOT used to compute new value.
    val sub1 = signal.addObserver(signalObserver1)

    calculations shouldEqual mutable.Buffer(
      Calculation("map-signal", -1) // First time current value of this signal was needed, so it is now calculated
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs-1", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("bus", 2),
      Calculation("map-signal", 20)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs-1", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      // signal should not propagate the same value
      // "map-signal" signal is derived from upstream signal which filters out the same value, so it doesn't get an update
      Calculation("bus", 2)
    )
    effects shouldEqual mutable.Buffer()

    calculations.clear()

    // --

    // When adding a new observer, it gets the signal's current value.
    // Here the current value has been updated by the previous event, and the signal remembers it.
    val sub2 = signal.addObserver(signalObserver2)

    calculations shouldEqual mutable.Buffer() // Using cached calculation
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs-2", 20)
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("bus", 3),
      Calculation("map-signal", 30)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs-1", 30),
      Effect("signal-obs-2", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    bus.writer.onNext(4)

    calculations shouldEqual mutable.Buffer(
      Calculation("bus", 4),
      Calculation("map-signal", 40)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs-2", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    sub2.kill()

    bus.writer.onNext(5)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    // Adding the observer again will work exactly the same as adding it initially
    signal.addObserver(signalObserver2)

    calculations shouldEqual mutable.Buffer() // Using cached calculation
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs-2", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(6)

    calculations shouldEqual mutable.Buffer(
      Calculation("bus", 6),
      Calculation("map-signal", 60)
    )
    effects shouldEqual mutable.Buffer(
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
    signal.changes shouldNot be(signal.changes)

    // --

    bus.writer.onNext(1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    val subChanges1 = changes.addObserver(changesObserver)

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("map-signal", -1),
      Calculation("bus", 2),
      Calculation("map-signal", 20),
      Calculation("changes", 20)
    )
    effects shouldEqual mutable.Buffer(
      Effect("changes-obs", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    // Adding observer to signal sends the last evaluated current value to it
    val subSignal = signal.addObserver(signalObserver)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", 20)
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("bus", 3),
      Calculation("map-signal", 30),
      Calculation("changes", 30)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", 30),
      Effect("changes-obs", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      // Unchanged value should not propagate through the Signal
      // map-signal is derived from an upstream signal that filters out same values, so it doesn't even get a calculation
      Calculation("bus", 3)
    )
    effects shouldEqual mutable.Buffer()

    calculations.clear()

    // --

    subChanges1.kill()

    bus.writer.onNext(4)

    calculations shouldEqual mutable.Buffer(
      Calculation("bus", 4),
      Calculation("map-signal", 40)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    val subChanges2 = changes.addObserver(changesObserver)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    subChanges2.kill()
    subSignal.kill()

    bus.writer.onNext(5)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
  }

  it("MapSignal.now/onNext combination does not redundantly evaluate project/initialValue") {

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

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    signal.tryNow() shouldBe Success(-1)

    calculations shouldEqual mutable.Buffer(
      Calculation("map-signal", -1)
    )
    effects shouldEqual mutable.Buffer()

    calculations.clear()

    // --

    signal.addObserver(signalObserver)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", -1)
    )

    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("bus", 2),
      Calculation("map-signal", 20)
    )
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    signal.tryNow()

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    signal.addObserver(Observer[Int](effects += Effect("signal-obs2", _)))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("signal-obs2", 20)
    )

    effects.clear()

    // --

    signal.tryNow()

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
  }

}
