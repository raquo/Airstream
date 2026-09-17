package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.ownership.{ManualOwner, Owner}
import com.raquo.airstream.state.{Val, Var}

import scala.collection.mutable

/** Tests for `flattenMerge` / `flatMapMerge`, i.e. `ConcurrentStream`.
  *
  * See also the "Flattening Observables" section of the README for the intended semantics.
  */
class FlattenMergeSpec extends UnitSpec {

  it("ConcurrentEventStream (input=stream)") {
    implicit val owner: Owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()

    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Int]
    val bus3 = new EventBus[Int]

    val stream1 = bus1.events.map(Calculation.log("stream1", calculations))
    val stream2 = bus2.events.map(Calculation.log("stream2", calculations))
    val stream3 = bus3.events.map(Calculation.log("stream3", calculations))

    val mergeBus = new EventBus[EventStream[Int]]

    val mergeStream = mergeBus.events.flattenMerge.map(Calculation.log("merge", calculations))

    val sub1 = mergeStream.addObserver(Observer.empty)

    calculations shouldBe mutable.Buffer()

    // --

    bus1.writer.onNext(0)
    calculations shouldBe mutable.Buffer()

    // --

    mergeBus.writer.onNext(stream1)
    calculations shouldBe mutable.Buffer()

    // --

    bus1.writer.onNext(1)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 1),
      Calculation("merge", 1)
    )
    calculations.clear()

    // --

    bus1.writer.onNext(2)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 2),
      Calculation("merge", 2)
    )
    calculations.clear()

    // --

    mergeBus.writer.onNext(stream2)
    mergeBus.writer.onNext(stream3)
    bus1.writer.onNext(3)
    bus2.writer.onNext(10)
    bus3.writer.onNext(100)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 3),
      Calculation("merge", 3),
      Calculation("stream2", 10),
      Calculation("merge", 10),
      Calculation("stream3", 100),
      Calculation("merge", 100)
    )
    calculations.clear()

    // --

    bus2.writer.onNext(20)
    calculations shouldBe mutable.Buffer(
      Calculation("stream2", 20),
      Calculation("merge", 20)
    )
    calculations.clear()

    // --

    sub1.kill()
    bus1.writer.onNext(4)
    calculations shouldBe mutable.Buffer()

    // --

    // We don't reset list of streams anymore

    mergeStream.addObserver(Observer.empty)
    bus1.writer.onNext(5)
    bus2.writer.onNext(30)
    bus3.writer.onNext(200)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 5),
      Calculation("merge", 5),
      Calculation("stream2", 30),
      Calculation("merge", 30),
      Calculation("stream3", 200),
      Calculation("merge", 200)
    )

    calculations.clear()

    // --

    mergeBus.writer.onNext(stream1)
    bus1.writer.onNext(6)
    bus1.writer.onNext(7)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 6),
      Calculation("merge", 6),
      Calculation("stream1", 7),
      Calculation("merge", 7)
    )
    calculations.clear()
  }

  it("ConcurrentStream (input=signal)") {
    implicit val owner: Owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()

    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Int]
    val bus3 = new EventBus[Int]

    val stream1 = bus1.events.map(Calculation.log("stream1", calculations))
    val stream2 = bus2.events.map(Calculation.log("stream2", calculations))
    val stream3 = bus3.events.map(Calculation.log("stream3", calculations))

    val streamVar = Var[EventStream[Int]](stream1)

    val mergeSignal = streamVar
      .signal
      .distinct
      .flattenMerge
      .map(Calculation.log("merge", calculations))

    val sub1 = mergeSignal.addObserver(Observer.empty)

    calculations shouldBe mutable.Buffer()

    // --

    bus1.writer.onNext(0) // writing to initial stream
    bus2.writer.onNext(-1) // writing to unrelated stream
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 0),
      Calculation("merge", 0)
    )
    calculations.clear()

    // --

    streamVar.writer.onNext(stream1)
    calculations shouldBe mutable.Buffer()

    // --

    bus1.writer.onNext(1)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 1),
      Calculation("merge", 1)
    )
    calculations.clear()

    // --

    bus1.writer.onNext(2)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 2),
      Calculation("merge", 2)
    )
    calculations.clear()

    // --

    streamVar.writer.onNext(stream2)
    streamVar.writer.onNext(stream3)
    bus1.writer.onNext(3)
    bus2.writer.onNext(10)
    bus3.writer.onNext(100)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 3),
      Calculation("merge", 3),
      Calculation("stream2", 10),
      Calculation("merge", 10),
      Calculation("stream3", 100),
      Calculation("merge", 100)
    )
    calculations.clear()

    // --

    bus2.writer.onNext(20)
    calculations shouldBe mutable.Buffer(
      Calculation("stream2", 20),
      Calculation("merge", 20)
    )
    calculations.clear()

    // --

    sub1.kill()
    bus1.writer.onNext(4)
    calculations shouldBe mutable.Buffer()

    // --

    // We don't reset the list of streams on stop anymore

    mergeSignal.addObserver(Observer.empty)
    bus1.writer.onNext(5)
    bus2.writer.onNext(30)
    bus3.writer.onNext(200) // `stream3` is current value of mergeSignal
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 5),
      Calculation("merge", 5),
      Calculation("stream2", 30),
      Calculation("merge", 30),
      Calculation("stream3", 200),
      Calculation("merge", 200)
    )
    calculations.clear()

    // --

    streamVar.writer.onNext(stream1) // Adding this stream a second time – there is no deduplication, that's why we see duplicate output events
    bus1.writer.onNext(6)
    bus1.writer.onNext(7)
    calculations shouldBe mutable.Buffer(
      Calculation("stream1", 6),
      Calculation("merge", 6),
      Calculation("stream1", 7),
      Calculation("merge", 7)
    )
    calculations.clear()
  }

  // https://github.com/raquo/Airstream/pull/158
  it("flattenMerge starts the initial inner stream that emits on start") {
    // Regression test: previously, when the parent was a Signal, the inner stream
    // provided by the signal's initial value was added to `accumulatedStreams` in
    // `onWillStart` but never had its own `willStart` phase run, so an inner stream
    // that emits on start (e.g. `EventStream.fromValue`) would silently fail to emit.
    val owner = new ManualOwner
    val received = mutable.Buffer.empty[Int]
    val parent: Signal[EventStream[Int]] = Val(EventStream.fromValue(42))
    val stream = parent.flattenMerge

    try {
      stream.foreach { value =>
        received += value
      }(using owner)

      // fromValue emits synchronously once the shared start transaction finishes.
      received.toList shouldBe List(42)
    } finally {
      owner.killSubscriptions()
    }
  }
}
