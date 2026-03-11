package com.raquo.airstream.scan

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class ScanLeftStreamSpec extends UnitSpec with BeforeAndAfter {

  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  private val errorCallback = (err: Throwable) => {
    errorEffects += Effect("unhandled", err)
    ()
  }

  val err1 = new Exception("err1")

  before {
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    errorEffects.clear()
  }

  // =========================================================================
  // Basic behavior
  // =========================================================================

  it("EventStream.reduceLeft does not emit before the first event") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val stream = bus.events.reduceLeft(_ + _)

    stream.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )

    effects.clear()

    // --

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(
      Effect("obs", 6)
    )
  }

  it("EventStream.reduceLeft: accumulated state persists across stop/start; events missed while stopped are lost") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val stream = bus.events.reduceLeft(_ + _)

    val sub = stream.addObserver(Observer[Int](effects += Effect("obs", _)))

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", 10),
      Effect("obs", 30)
    )

    effects.clear()

    // Stop observing
    sub.kill()

    // This event is emitted while stopped — it is NOT accumulated into state
    bus.writer.onNext(5)

    // Re-subscribe — the underlying signal remembers the last accumulated value (30)
    stream.addObserver(Observer[Int](effects += Effect("obs", _)))

    // No immediate emission (this is a stream, not a signal)
    effects shouldBe mutable.Buffer()

    // The next event accumulates on top of the preserved state (30), NOT from 5 (which was lost)
    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(
      Effect("obs", 31) // 30 + 1, not 5 + 1 (5 was lost) and not 1 (state persisted)
    )
  }

  it("EventStream.scanLeft: accumulated state persists across stop/start; events between are ignored") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0)(_ + _)

    val sub = signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    // Initial value from seed
    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", 10),
      Effect("obs", 30)
    )
    effects.clear()

    // Stop observing
    sub.kill()

    // These events are emitted while stopped — they are NOT accumulated
    bus.writer.onNext(5)
    bus.writer.onNext(7)

    // Re-subscribe — the signal emits its preserved accumulated value (30, not 42)
    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 30))
    effects.clear()

    // The next event accumulates from 30; the missed events (5, 7) were truly lost
    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 31))
  }

  it("EventStream.reduceLeft supports type widening") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Any]]()
    val bus = new EventBus[Int]

    // reduceLeft[B >: A] — accumulate into a String (supertype via Any)
    val stream: com.raquo.airstream.core.EventStream[Any] =
      bus.events.reduceLeft[Any]((acc, next) => s"$acc+$next")

    stream.addObserver(Observer[Any](effects += Effect("obs", _)))

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "1+2"))
    effects.clear()

    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "1+2+3"))
  }

  it("EventStream.reduceLeftOption starts with None and accumulates into Some") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Option[Int]]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftOption[Int](_ + _)

    signal.addObserver(Observer[Option[Int]](effects += Effect("obs", _)))

    // Initial value before any events
    effects shouldBe mutable.Buffer(
      Effect("obs", None)
    )

    effects.clear()

    // --

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(
      Effect("obs", Some(1))
    )

    effects.clear()

    // --

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(
      Effect("obs", Some(3))
    )

    effects.clear()

    // --

    bus.writer.onNext(10)

    effects shouldBe mutable.Buffer(
      Effect("obs", Some(13))
    )
  }

  it("EventStream.reduceLeftDefault starts with default and then accumulates") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftDefault(0)(_ + _)

    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    // Default value before any events
    effects shouldBe mutable.Buffer(
      Effect("obs", 0)
    )

    effects.clear()

    // --

    bus.writer.onNext(5)

    // Default is NOT incorporated into the accumulation — the first event is the seed
    effects shouldBe mutable.Buffer(
      Effect("obs", 5)
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(
      Effect("obs", 8)
    )
  }

  it("EventStream.reduceLeftDefault uses default lazily") {

    implicit val owner: TestableOwner = new TestableOwner

    var defaultEvalCount = 0

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftDefault {
      defaultEvalCount += 1
      -1
    }(_ + _)

    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    // Default is evaluated once to produce the signal's initial value
    defaultEvalCount shouldBe 1

    bus.writer.onNext(1)

    // After the first event, default is no longer needed
    defaultEvalCount shouldBe 1

    effects shouldBe mutable.Buffer(
      Effect("obs", -1),
      Effect("obs", 1)
    )
  }

  // =========================================================================
  // Error handling
  // =========================================================================

  it("EventStream.reduceLeft propagates exceptions as errors; error state persists in accumulator") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val stream = bus.events.reduceLeft[Int] { (acc, next) =>
      if (next < 0) throw err1
      acc + next
    }

    stream.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", 0)
    ))

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    bus.writer.onNext(-5)

    // fn throws, so the underlying signal enters Failure state
    effects shouldBe mutable.Buffer(Effect("obs-err", 0))
    effects.clear()

    // The accumulated state is now Failure(err1). Subsequent events call fn(Failure.get, next.get)
    // which rethrows err1 before even reaching the user's fn, so the error keeps propagating.
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs-err", 0))
  }

  it("EventStream.reduceLeftOption: fn error persists; re-emitted on each subsequent event") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Option[Int]]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftOption[Int] { (acc, next) =>
      if (next < 0) throw err1
      acc + next
    }

    signal.addObserver(Observer.withRecover[Option[Int]](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", None)
    ))

    // Initial None before any events
    effects shouldBe mutable.Buffer(Effect("obs", None))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", Some(1)))
    effects.clear()

    // fn throws — signal enters error state
    bus.writer.onNext(-5)

    effects shouldBe mutable.Buffer(Effect("obs-err", None))
    effects.clear()

    // Error persists — each subsequent event re-emits the error
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs-err", None))
  }

  it("EventStream.reduceLeftDefault: fn error persists; re-emitted on each subsequent event") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftDefault(0) { (acc, next) =>
      if (next < 0) throw err1
      acc + next
    }

    signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", 0)
    ))

    // Default value before any events
    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    effects.clear()

    // fn throws — signal enters error state
    bus.writer.onNext(-1)

    effects shouldBe mutable.Buffer(Effect("obs-err", 0))
    effects.clear()

    // Error persists — each subsequent event re-emits the error
    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs-err", 0))
  }

  // =========================================================================
  // resetOnStop
  // =========================================================================

  it("EventStream.scanLeft(resetOnStop=true): resets to initial seed on restart; no state persistence") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0, resetOnStop = true)(_ + _)

    val sub = signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    // Initial value from seed
    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", 10),
      Effect("obs", 30)
    )
    effects.clear()

    // Stop observing
    sub.kill()

    // Events while stopped are lost
    bus.writer.onNext(5)

    // Re-subscribe — with resetOnStop=true the accumulated value resets to the initial seed (0)
    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    // Next event accumulates from the reset seed, NOT from the old accumulated value
    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", 3))
  }

  it("EventStream.scanLeft(resetOnStop=false): default — accumulated state persists across stop/start") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0, resetOnStop = false)(_ + _)

    val sub = signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", 10),
      Effect("obs", 30)
    )
    effects.clear()

    sub.kill()

    bus.writer.onNext(5)

    // Re-subscribe — accumulated value (30) is preserved despite events being missed
    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 30))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 31))
  }

  it("EventStream.reduceLeft(resetOnStop=true): resets state on restart; first post-restart event is the new seed") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val stream = bus.events.reduceLeft(_ + _, resetOnStop = true)

    val sub = stream.addObserver(Observer[Int](effects += Effect("obs", _)))

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", 10),
      Effect("obs", 30)
    )
    effects.clear()

    // Stop observing
    sub.kill()

    // Re-subscribe — no immediate emission (stream, not signal)
    stream.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer()

    // State has been reset; first event is the new seed (not 30 + 1 = 31)
    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 1))
  }

  it("EventStream.reduceLeft(resetOnStop=false): default — state persists; first post-restart event accumulates") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val stream = bus.events.reduceLeft(_ + _, resetOnStop = false)

    val sub = stream.addObserver(Observer[Int](effects += Effect("obs", _)))

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", 10),
      Effect("obs", 30)
    )
    effects.clear()

    sub.kill()

    stream.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer()

    // State (30) is preserved; next event accumulates from it
    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 31))
  }

  it("EventStream.reduceLeftOption(resetOnStop=true): resets to None on restart; first event is new seed") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Option[Int]]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftOption[Int](_ + _, resetOnStop = true)

    val sub = signal.addObserver(Observer[Option[Int]](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", None))
    effects.clear()

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", Some(10)),
      Effect("obs", Some(30)),
    )
    effects.clear()

    sub.kill()

    // Re-subscribe — resetOnStop=true resets to None (the scan seed)
    signal.addObserver(Observer[Option[Int]](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", None))
    effects.clear()

    // First event after reset becomes the new seed (not 30 + 1 = 31)
    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", Some(1)))
  }

  it("EventStream.reduceLeftDefault(resetOnStop=true): resets to default on restart; first event is new seed") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftDefault(0, resetOnStop = true)(_ + _)

    val sub = signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    // Default value shown before any events
    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("obs", 10),
      Effect("obs", 30),
    )
    effects.clear()

    sub.kill()

    // Re-subscribe — state resets; default (0) is shown again
    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    // First event after reset is the new seed (not 30 + 1 = 31)
    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 1))
  }

  // =========================================================================
  // skipErrors
  // =========================================================================

  it("EventStream.scanLeft(skipErrors=true): upstream errors are skipped; accumulated state is preserved and re-emitted") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0, skipErrors = true)(_ + _)

    signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    // Initial value from seed
    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    // Upstream error — skipped by skipErrors; current value (1) is preserved and re-emitted
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    // Accumulation continues from the preserved value (1)
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", 3))
    errorEffects shouldBe mutable.Buffer()
  }

  it("EventStream.scanLeft(skipErrors=false): default — upstream errors replace accumulated state and persist") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0, skipErrors = false)(_ + _)

    signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    // Upstream error — NOT skipped; signal enters error state
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
    effects.clear()

    // Error state persists; subsequent events re-emit the error
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
  }

  it("EventStream.scanLeft(skipErrors=true): combine errors are also skipped; accumulated state is preserved") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0, skipErrors = true) { (acc, next) =>
      if (next < 0) throw err1
      acc + next
    }

    signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    effects.clear()

    // combine throws — error is caught and discarded; current value (5) is preserved and re-emitted
    bus.writer.onNext(-1)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    // Accumulation continues from the preserved value (5)
    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", 8))
  }

  it("EventStream.scanLeft(skipErrors=false): default — combine errors replace accumulated state") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0, skipErrors = false) { (acc, next) =>
      if (next < 0) throw err1
      acc + next
    }

    signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    effects.clear()

    bus.writer.onNext(-1)

    // combine throws → signal enters error state
    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
    effects.clear()

    // Error state persists
    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
  }

  it("EventStream.reduceLeft(skipErrors=true): upstream errors are skipped; state is preserved") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val stream = bus.events.reduceLeft(_ + _, skipErrors = true)

    stream.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    bus.writer.onNext(10)

    effects shouldBe mutable.Buffer(Effect("obs", 10))
    effects.clear()

    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(Effect("obs", 30))
    effects.clear()

    // Upstream error — the underlying option signal preserves Some(30) and re-fires it,
    // which passes through .collect { case Some(v) => v } and re-emits 30
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs", 30))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    // Accumulation continues from the preserved state (30)
    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 35))
  }

  it("EventStream.reduceLeft(skipErrors=false): default — upstream errors are propagated") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val stream = bus.events.reduceLeft(_ + _, skipErrors = false)

    stream.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    bus.writer.onNext(10)

    effects shouldBe mutable.Buffer(Effect("obs", 10))
    effects.clear()

    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
  }

  it("EventStream.reduceLeftOption(skipErrors=true): upstream errors are skipped; Option state is preserved") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Option[Int]]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftOption[Int](_ + _, skipErrors = true)

    signal.addObserver(Observer[Option[Int]](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", None))
    effects.clear()

    bus.writer.onNext(10)

    effects shouldBe mutable.Buffer(Effect("obs", Some(10)))
    effects.clear()

    // Upstream error — skipErrors preserves Some(10) and re-emits it
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs", Some(10)))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", Some(15)))
  }

  it("EventStream.reduceLeftDefault(skipErrors=true): upstream errors skip; default shown before first success") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftDefault(0, skipErrors = true)(_ + _)

    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    // Default value before any events
    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    effects.clear()

    // Upstream error — skipErrors preserves Some(5) internally; map to 5 via getOrElse
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", 8))
  }

  // =========================================================================
  // resetOnStop + skipErrors combined
  // =========================================================================

  it("EventStream.scanLeft(resetOnStop=true, skipErrors=true): both flags work together") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signal = bus.events.scanLeft(0, resetOnStop = true, skipErrors = true)(_ + _)

    val sub = signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(5)

    // Upstream error — skipped, 5 preserved and re-emitted
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(
      Effect("obs", 5),
      Effect("obs", 5),
    )
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    sub.kill()

    // Re-subscribe — reset to initial seed (0), ignoring preserved value (5)
    signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", 2))
  }
}
