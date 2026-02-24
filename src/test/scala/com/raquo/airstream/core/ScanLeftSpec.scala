package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}

import scala.collection.mutable

class ScanLeftSpec extends UnitSpec {

  it("reduceLeft - accumulates events using a binary operator") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val bus = new EventBus[Int]
    val stream = bus.events.reduceLeft(_ + _)

    val sub = stream.addObserver(Effect.logObserver("obs", effects))

    bus.writer.onNext(1)

    effects.toList shouldBe List(
      Effect("obs", 1)
    )
    effects.clear()

    // --

    bus.writer.onNext(2)

    effects.toList shouldBe List(
      Effect("obs", 3)
    )
    effects.clear()

    // --

    bus.writer.onNext(3)

    effects.toList shouldBe List(
      Effect("obs", 6)
    )
    effects.clear()

    // --

    sub.kill()
    bus.writer.onNext(10)

    stream.addObserver(Effect.logObserver("obs2", effects))

    // Accumulated state is preserved across restart; 10 was lost because the stream was stopped
    effects.toList shouldBe Nil

    bus.writer.onNext(5)

    effects.toList shouldBe List(
      Effect("obs2", 11) // 6 (accumulated) + 5
    )
    effects.clear()
  }

  it("reduceLeft - first event is emitted as-is") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()

    val bus = new EventBus[String]
    val stream = bus.events.reduceLeft(_ + _)

    stream.addObserver(Effect.logObserver("obs", effects))

    bus.writer.onNext("hello")

    effects.toList shouldBe List(
      Effect("obs", "hello")
    )
    effects.clear()

    // --

    bus.writer.onNext(" world")

    effects.toList shouldBe List(
      Effect("obs", "hello world")
    )
    effects.clear()
  }

  // --

  it("count - counts events emitted by the parent stream") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Long]]()

    val bus = new EventBus[String]
    val signal = bus.events.count

    val sub = signal.addObserver(Effect.logObserver("obs", effects))

    // Initial value before any events
    effects.toList shouldBe List(
      Effect("obs", 0L)
    )
    effects.clear()

    // --

    bus.writer.onNext("a")

    effects.toList shouldBe List(
      Effect("obs", 1L)
    )
    effects.clear()

    // --

    bus.writer.onNext("b")
    bus.writer.onNext("c")

    effects.toList shouldBe List(
      Effect("obs", 2L),
      Effect("obs", 3L),
    )
    effects.clear()

    // --

    sub.kill()
    bus.writer.onNext("missed")

    signal.addObserver(Effect.logObserver("obs2", effects))

    // Accumulated state is preserved across restart; the missed event was lost because the stream was stopped
    effects.toList shouldBe List(
      Effect("obs2", 3L) // signal re-emits its preserved current value on subscribe
    )
    effects.clear()

    bus.writer.onNext("d")

    effects.toList shouldBe List(
      Effect("obs2", 4L)
    )
    effects.clear()
  }

  // --

  it("zipWithIndex - pairs each event with its 0-based index") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[(String, Long)]]()

    val bus = new EventBus[String]
    val stream = bus.events.zipWithIndex

    stream.addObserver(Effect.logObserver("obs", effects))

    bus.writer.onNext("a")

    effects.toList shouldBe List(
      Effect("obs", ("a", 0L))
    )
    effects.clear()

    // --

    bus.writer.onNext("b")

    effects.toList shouldBe List(
      Effect("obs", ("b", 1L))
    )
    effects.clear()

    // --

    bus.writer.onNext("c")

    effects.toList shouldBe List(
      Effect("obs", ("c", 2L))
    )
    effects.clear()
  }

  it("zipWithIndex - index is preserved across restart") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[(Int, Long)]]()

    val bus = new EventBus[Int]
    val stream = bus.events.zipWithIndex

    val sub = stream.addObserver(Effect.logObserver("obs", effects))

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects.toList shouldBe List(
      Effect("obs", (10, 0L)),
      Effect("obs", (20, 1L)),
    )
    effects.clear()

    // --

    sub.kill()
    bus.writer.onNext(30) // missed while stopped

    stream.addObserver(Effect.logObserver("obs2", effects))

    bus.writer.onNext(40)

    // Accumulated state is preserved; index continues from where it left off
    effects.toList shouldBe List(
      Effect("obs2", (40, 2L))
    )
    effects.clear()
  }

  // --

  it("filterIndex - emits only events whose index satisfies the predicate") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    // Keep only events at even indices (0, 2, 4, ...)
    val stream = EventStream
      .fromSeq(List(10, 20, 30, 40, 50))
      .filterIndex(_ % 2 == 0)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", 10),
      Effect("obs", 30),
      Effect("obs", 50),
    )
    effects.clear()
  }

  it("filterIndex - keeps only odd-indexed events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()

    val stream = EventStream
      .fromSeq(List("a", "b", "c", "d", "e"))
      .filterIndex(_ % 2 == 1)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", "b"),
      Effect("obs", "d"),
    )
    effects.clear()
  }

  // --

  it("stride - emits every step-th event starting from the first") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val stream = EventStream
      .fromSeq(List(1, 2, 3, 4, 5, 6))
      .stride(step = 2)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", 1),
      Effect("obs", 3),
      Effect("obs", 5),
    )
    effects.clear()
  }

  it("stride - step=1 is a no-op") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val stream = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .stride(step = 1)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", 1),
      Effect("obs", 2),
      Effect("obs", 3),
      Effect("obs", 4),
    )
    effects.clear()
  }

  it("stride - with offset skips initial events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val stream = EventStream
      .fromSeq(List(1, 2, 3, 4, 5, 6, 7))
      .stride(step = 3, offset = 1)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", 2),
      Effect("obs", 5),
    )
    effects.clear()
  }

  it("stride - step <= 0 throws IllegalArgumentException") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    intercept[IllegalArgumentException] {
      EventStream.fromSeq(List(1, 2, 3)).stride(step = 0)
    }

    intercept[IllegalArgumentException] {
      EventStream.fromSeq(List(1, 2, 3)).stride(step = -1)
    }
  }

  // --

  it("sliding - emits windows of the last N events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val bus = new EventBus[Int]
    val stream = bus.events.sliding(3)

    stream.addObserver(Effect.logObserver("obs", effects))

    bus.writer.onNext(1)
    bus.writer.onNext(2)

    // Warmup events are dropped by default (need full window of 3)
    effects.toList shouldBe Nil

    bus.writer.onNext(3)

    effects.toList shouldBe List(
      Effect("obs", Seq(1, 2, 3))
    )
    effects.clear()

    // --

    bus.writer.onNext(4)

    effects.toList shouldBe List(
      Effect("obs", Seq(2, 3, 4))
    )
    effects.clear()

    // --

    bus.writer.onNext(5)

    effects.toList shouldBe List(
      Effect("obs", Seq(3, 4, 5))
    )
    effects.clear()
  }

  it("sliding - includeWarmup=true includes partial windows") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val stream = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .sliding(3, includeWarmup = true)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", Seq(1)),
      Effect("obs", Seq(1, 2)),
      Effect("obs", Seq(1, 2, 3)),
      Effect("obs", Seq(2, 3, 4)),
    )
    effects.clear()
  }

  it("sliding - size=0 always emits empty sequences") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val stream = EventStream
      .fromSeq(List(1, 2, 3))
      .sliding(0)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", Seq.empty),
      Effect("obs", Seq.empty),
      Effect("obs", Seq.empty),
    )
    effects.clear()
  }

  it("sliding - size=1 emits single-element sequences") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val stream = EventStream
      .fromSeq(List(10, 20, 30))
      .sliding(1)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", Seq(10)),
      Effect("obs", Seq(20)),
      Effect("obs", Seq(30)),
    )
    effects.clear()
  }

  it("sliding - with step>1 emits every step-th window") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val stream = EventStream
      .fromSeq(List(1, 2, 3, 4, 5, 6))
      .sliding(size = 2, step = 2)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", Seq(1, 2)),
      Effect("obs", Seq(3, 4)),
      Effect("obs", Seq(5, 6)),
    )
    effects.clear()
  }

  it("sliding - negative size throws IllegalArgumentException") {

    implicit val owner: TestableOwner = new TestableOwner

    intercept[IllegalArgumentException] {
      EventStream.fromSeq(List(1, 2, 3)).sliding(-1)
    }
  }

  // --

  it("pairs - emits consecutive event pairs") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[(Int, Int)]]()

    val bus = new EventBus[Int]
    val stream = bus.events.pairs

    stream.addObserver(Effect.logObserver("obs", effects))

    bus.writer.onNext(1)

    // First event has no previous event to pair with
    effects.toList shouldBe Nil

    bus.writer.onNext(2)

    effects.toList shouldBe List(
      Effect("obs", (1, 2))
    )
    effects.clear()

    // --

    bus.writer.onNext(3)

    effects.toList shouldBe List(
      Effect("obs", (2, 3))
    )
    effects.clear()

    // --

    bus.writer.onNext(4)

    effects.toList shouldBe List(
      Effect("obs", (3, 4))
    )
    effects.clear()
  }

  // --

  it("grouped - emits non-overlapping groups of N events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val stream = EventStream
      .fromSeq(List(1, 2, 3, 4, 5, 6))
      .grouped(3)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", Seq(1, 2, 3)),
      Effect("obs", Seq(4, 5, 6)),
    )
    effects.clear()
  }

  it("grouped - incomplete last group is not emitted") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val bus = new EventBus[Int]
    val stream = bus.events.grouped(3)

    stream.addObserver(Effect.logObserver("obs", effects))

    bus.writer.onNext(1)
    bus.writer.onNext(2)

    effects.toList shouldBe Nil

    bus.writer.onNext(3)

    effects.toList shouldBe List(
      Effect("obs", Seq(1, 2, 3))
    )
    effects.clear()

    // --

    bus.writer.onNext(4)
    bus.writer.onNext(5)

    effects.toList shouldBe Nil

    bus.writer.onNext(6)

    effects.toList shouldBe List(
      Effect("obs", Seq(4, 5, 6))
    )
    effects.clear()
  }

  it("grouped - size=1 emits each event as a singleton group") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Seq[String]]]()

    val stream = EventStream
      .fromSeq(List("x", "y", "z"))
      .grouped(1)

    stream.addObserver(Effect.logObserver("obs", effects))

    effects.toList shouldBe List(
      Effect("obs", Seq("x")),
      Effect("obs", Seq("y")),
      Effect("obs", Seq("z")),
    )
    effects.clear()
  }
}
