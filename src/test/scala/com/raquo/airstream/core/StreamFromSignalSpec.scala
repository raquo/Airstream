package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class StreamFromSignalSpec extends UnitSpec {

  // -- signal.updates

  it("Signal.updates lazily reflects the changes of underlying signal") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val signalObserver = Observer[Int](effects += Effect("signal-obs", _))
    val updatesObserver = Observer[Int](effects += Effect("updates-obs", _))

    val bus = new EventBus[Int]
    val signal = bus.events
      .map(Calculation.log("bus", calculations))
      .map(_ * 10)
      .toSignal(initial = -1)
      .map(Calculation.log("map-signal", calculations))
    val updates = signal.updates.map(Calculation.log("updates", calculations))

    // .updates can't be a lazy val for memory management purposes
    // (parent should not have a reference to a child that has no observers)
    // #TODO is this actually a legit concern?
    //  - This simply links the two observables together for GC purposes
    //  - GC should still be able to eliminate them when both of them are
    //    no longer referenced
    //  - If .updates is a def, in some cases we could GC it sooner
    //    than if it was linked, but on the flip side, now we need to create
    //    a new observable for every consumer of .updates, instead of reusing it.
    signal.updates shouldNotBe signal.updates

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val subUpdates1 = updates.addObserver(updatesObserver)

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("map-signal", -1),
      Calculation("bus", 2),
      Calculation("map-signal", 20),
      Calculation("updates", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("updates-obs", 20)
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
      Calculation("updates", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 30),
      Effect("updates-obs", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 3),
      Calculation("map-signal", 30),
      Calculation("updates", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 30),
      Effect("updates-obs", 30)
    )

    calculations.clear()
    effects.clear()

    // --

    subUpdates1.kill()

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

    // The updates stream missed a signal update (detected using parent.lastUpdateId),
    // so when it's restarted, it emits the parent's new current value.

    val subUpdates2 = updates.addObserver(updatesObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("updates", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("updates-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    subUpdates2.kill()

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

    val subUpdates3 = updates.addObserver(updatesObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("updates", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("updates-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // --

    subUpdates3.kill()

    // Meanwhile if we the parent signal does not emit any updates while the
    // updates stream is stopped, the updates stream does not re-emit the
    // parent's current value when re-starting.

    val subUpdates4 = updates.addObserver(updatesObserver)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    subUpdates4.kill()
    subSignal.kill()

    bus.writer.onNext(5)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
  }

  // -- signal.toStream --

  it("Signal.toStream emits current value on start (unlike Signal.updates)") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val _var = Var(1)

    val updatesObs = Observer[Int](effects += Effect("updates", _))
    val toStreamObs = Observer[Int](effects += Effect("to-stream", _))

    // -- `updates` does NOT emit the signal's current value on start

    val updatesSub = _var.signal.updates.addObserver(updatesObs)

    effects shouldBe mutable.Buffer()

    // -- `toStream` DOES emit the signal's current value on start,
    //    even though the signal's value has never been updated
    //    (i.e. even though the parent signal's lastUpdateId is still 0).

    val toStream = _var.signal.toStream
    val toStreamSub1 = toStream.addObserver(toStreamObs)

    effects shouldBe mutable.Buffer(
      Effect("to-stream", 1)
    )
    effects.clear()

    // -- Subsequent updates are emitted by both streams

    _var.set(2)

    effects shouldBe mutable.Buffer(
      Effect("updates", 2),
      Effect("to-stream", 2)
    )
    effects.clear()

    // -- Restarting `toStream` without a change while stopped does NOT
    //    re-emit the current value: it has already emitted, and the parent
    //    has not updated while it was stopped. This matches `updates`.

    toStreamSub1.kill()

    val toStreamSub2 = toStream.addObserver(toStreamObs)

    effects shouldBe mutable.Buffer()

    // -- But if the parent updates while `toStream` is stopped, it re-emits
    //    the new current value on restart (same syncing behaviour as `updates`).

    toStreamSub2.kill()

    _var.set(3)

    val toStreamSub3 = toStream.addObserver(toStreamObs)

    effects shouldBe mutable.Buffer(
      Effect("updates", 3), // `updates` is still subscribed, so it emits synchronously on set
      Effect("to-stream", 3)
    )
    effects.clear()

    toStreamSub3.kill()
    updatesSub.kill()
  }

  it("Signal.toStream lazily reflects the changes of underlying signal") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val signalObserver = Observer[Int](effects += Effect("signal-obs", _))
    val streamObserver = Observer[Int](effects += Effect("stream-obs", _))

    val bus = new EventBus[Int]
    val signal = bus.events
      .map(Calculation.log("bus", calculations))
      .map(_ * 10)
      .toSignal(initial = -1)
      .map(Calculation.log("map-signal", calculations))
    val stream = signal.toStream.map(Calculation.log("stream", calculations))

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // -- Starting the stream evaluates the signal's initial value and emits it

    val subStream1 = stream.addObserver(streamObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("map-signal", -1),
      Calculation("stream", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("stream-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("bus", 2),
      Calculation("map-signal", 20),
      Calculation("stream", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("stream-obs", 20)
    )

    calculations.clear()
    effects.clear()

    // -- Adding observer to signal sends the last evaluated current value to it

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
      Calculation("stream", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", 30),
      Effect("stream-obs", 30)
    )

    calculations.clear()
    effects.clear()

    // -- The stream missed a signal update while stopped,
    //    so when it's restarted, it emits the parent's new current value.

    subStream1.kill()

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

    val subStream2 = stream.addObserver(streamObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 40)
    )
    effects shouldBe mutable.Buffer(
      Effect("stream-obs", 40)
    )

    calculations.clear()
    effects.clear()

    // -- If the parent signal does not emit any updates while the stream is
    //    stopped, the stream does not re-emit the parent's current value when
    //    re-starting (it has already emitted, so this matches `updates`).

    subStream2.kill()

    val subStream3 = stream.addObserver(streamObserver)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    subStream3.kill()
    subSignal.kill()

    bus.writer.onNext(5)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
  }
}
