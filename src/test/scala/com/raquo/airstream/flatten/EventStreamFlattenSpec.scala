package com.raquo.airstream.flatten

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.flatten.FlattenStrategy.ConcurrentStreamStrategy
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.state.Var

import scala.collection.mutable

class EventStreamFlattenSpec extends AsyncUnitSpec {

  private val done = assert(true)

  it("sync map-flatten") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .map { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true)
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3))
  }

  it("sync three-level map-flatten") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .map { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true).map { vv =>
            EventStream.fromSeq(Seq(vv * 7), emitOnce = true)
          }.flatten
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3 * 7))
  }

  private def delayedStream(points: Seq[Int], interval: Int, valueF: Int => Int = identity): EventStream[Int] = {
    val bus = new EventBus[Int]()
    points.foreach { i =>
      delay(i * interval) {
        bus.writer.onNext(valueF(i))
      }
    }
    bus.events
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("from-future map-flatten") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 30)

    val flatStream =
      stream
        .map { v =>
          delayedStream(range2, interval = 6, _ * v)
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(150) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j =>
          Effect("obs0", i * j)
        )

      )
    }
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("three-level from-future map-flatten") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .map { v =>
          delayedStream(range2, interval = 6, _ * v).map { vv =>
            EventStream.fromFuture(delay(1)(vv * 7))
          }.flatten
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j => Effect("obs0", i * j * 7))
      )
    }
  }

  it("sync flatMap") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .flatMap { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true)
        }

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3))
  }

  it("sync three-level flatMap") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .flatMap { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true).flatMap { vv =>
            EventStream.fromSeq(Seq(vv * 7), emitOnce = true)
          }
        }

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3 * 7))
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("from-future flatMap") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .flatMap { v =>
          delayedStream(range2, interval = 6, _ * v)
        }

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j =>
          Effect("obs0", i * j)
        )

      )
    }
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("three-level from-future flatMap") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .flatMap { v =>
          delayedStream(range2, interval = 6, _ * v).flatMap { vv =>
            EventStream.fromFuture(delay(1)(vv * 7))
          }
        }

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j => Effect("obs0", i * j * 7))
      )
    }
  }

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

    val mergeStream = mergeBus.events.flatten(ConcurrentStreamStrategy).map(Calculation.log("merge", calculations))

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

    done
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
      .flatten(ConcurrentStreamStrategy)
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

    done
  }

}
