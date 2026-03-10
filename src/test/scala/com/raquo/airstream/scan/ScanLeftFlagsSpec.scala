package com.raquo.airstream.scan

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

/**
 * Tests for the `resetOnStop` and `skipErrors` flags on scan/reduce operators.
 *
 * `resetOnStop = true` — the accumulator is reset to the initial value when the observable is stopped and restarted.
 * `skipErrors = true`  — upstream and combine errors are silently skipped; the accumulator preserves its previous state.
 */
class ScanLeftFlagsSpec extends UnitSpec with BeforeAndAfter {

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

    val stream = bus.events.reduceLeft(resetOnStop = true)(_ + _)

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

    val stream = bus.events.reduceLeft(resetOnStop = false)(_ + _)

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

  it("Signal.reduceLeft(resetOnStop=true): resets to current parent value on restart") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val v = Var(1)

    val signal = v.signal.reduceLeft(resetOnStop = true)(_ + _)

    val sub = signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    v.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 6))
    effects.clear()

    sub.kill()

    // Upstream emits while stopped
    v.writer.onNext(10)

    // Re-subscribe — with resetOnStop=true the signal uses makeInitial(parent.now) = parent.now = 10
    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 10))
    effects.clear()

    v.writer.onNext(3)

    // Accumulates from the reset value (10), not the old accumulated value (6)
    effects shouldBe mutable.Buffer(Effect("obs", 13))
  }

  it("Signal.reduceLeft(resetOnStop=false): default — re-syncs accumulated state with upstream changes") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val v = Var(1)

    val signal = v.signal.reduceLeft(resetOnStop = false)(_ + _)

    val sub = signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    v.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 6))
    effects.clear()

    sub.kill()

    v.writer.onNext(10)

    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    // Re-syncs: combine(lastAccumulated=6, parent.now=10) = 16
    effects shouldBe mutable.Buffer(Effect("obs", 16))
    effects.clear()

    v.writer.onNext(4)

    effects shouldBe mutable.Buffer(Effect("obs", 20))
  }

  it("Signal.scanLeft with makeInitial (resetOnStop=true): re-evaluates makeInitial with current parent on restart") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    val signal = v.signal.scanLeft(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = true,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    val sub = signal.addObserver(Observer[String](effects += Effect("obs", _)))

    // makeInitial(1) = "start:1"
    effects shouldBe mutable.Buffer(Effect("obs", "start:1"))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "start:1 2"))
    effects.clear()

    sub.kill()

    v.writer.onNext(10)

    // Re-subscribe — makeInitial is re-evaluated with current parent value (10)
    signal.addObserver(Observer[String](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", "start:10"))
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "start:10 3"))
  }

  it("Signal.scanLeft with makeInitial (resetOnStop=false): default — re-syncs accumulated value with upstream") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    val signal = v.signal.scanLeft(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    val sub = signal.addObserver(Observer[String](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", "start:1"))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "start:1 2"))
    effects.clear()

    sub.kill()

    v.writer.onNext(10)

    // Re-subscribe — re-syncs: fn("start:1 2", 10) = "start:1 2 10"
    signal.addObserver(Observer[String](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", "start:1 2 10"))
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "start:1 2 10 3"))
  }

  it("Signal.scanLeft with constant initial (resetOnStop=true): re-applies seed to current parent value on restart") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    val signal = v.signal.scanLeft("n:", resetOnStop = true) { (acc, n) => s"$acc $n" }

    val sub = signal.addObserver(Observer[String](effects += Effect("obs", _)))

    // combine("n:", 1) = "n: 1"
    effects shouldBe mutable.Buffer(Effect("obs", "n: 1"))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "n: 1 2"))
    effects.clear()

    sub.kill()

    v.writer.onNext(10)

    // Re-subscribe — reset: re-apply seed to current parent value → combine("n:", 10) = "n: 10"
    signal.addObserver(Observer[String](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", "n: 10"))
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "n: 10 3"))
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

    val stream = bus.events.reduceLeft(skipErrors = true)(_ + _)

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

    val stream = bus.events.reduceLeft(skipErrors = false)(_ + _)

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

  it("Signal.scanLeft(skipErrors=true): upstream errors from a signal parent are skipped; state is preserved") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val bus = new EventBus[Int]

    // Parent is a signal that can emit errors
    val parentSignal = bus.events.startWith(0)
    val result = parentSignal.scanLeft(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = true,
    ) { (acc, n) => s"$acc $n" }

    result.addObserver(Observer.withRecover[String](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", "err"),
    ))

    // makeInitial(0) = "start:0"
    effects shouldBe mutable.Buffer(Effect("obs", "start:0"))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1"))
    effects.clear()

    // Parent signal emits error — skipErrors preserves "start:0 1" and re-emits it
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1"))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    // Accumulation continues from preserved state
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1 2"))
  }

  it("Signal.scanLeft(skipErrors=false): default — upstream errors from a signal parent are propagated") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)
    val result = parentSignal.scanLeft(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    result.addObserver(Observer.withRecover[String](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", "err"),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", "start:0"))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1"))
    effects.clear()

    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs-err", "err"))
    effects.clear()

    // Error persists on subsequent events
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs-err", "err"))
  }

  it("Signal.reduceLeft(skipErrors=true): upstream errors are skipped; accumulated state is preserved") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)
    val result = parentSignal.reduceLeft(skipErrors = true)(_ + _)

    result.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    // Initial: identity(parent.now) = 0
    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    effects.clear()

    // Upstream error — skipErrors preserves 5 and re-emits it
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    // Accumulation continues from preserved state (5)
    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", 8))
  }

  it("Signal.reduceLeft(skipErrors=false): default — upstream errors are propagated through the signal") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)
    val result = parentSignal.reduceLeft(skipErrors = false)(_ + _)

    result.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", -1),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    effects.clear()

    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
    effects.clear()

    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs-err", -1))
  }

  it("Signal.scanLeft(skipErrors=true): combine errors are skipped; accumulated state is preserved") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)
    val result = parentSignal.scanLeft(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = true,
    ) { (acc, n) =>
      if (n < 0) throw err1
      s"$acc $n"
    }

    result.addObserver(Observer.withRecover[String](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", "err"),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", "start:0"))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1"))
    effects.clear()

    // combine throws for negative input — skipErrors preserves "start:0 1" and re-emits it
    bus.writer.onNext(-1)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1"))
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    // Accumulation continues from the preserved state
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1 2"))
  }

  it("Signal.scanLeft(skipErrors=false): default — combine errors replace accumulated state") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)
    val result = parentSignal.scanLeft(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = false,
    ) { (acc, n) =>
      if (n < 0) throw err1
      s"$acc $n"
    }

    result.addObserver(Observer.withRecover[String](
      effects += Effect("obs", _),
      _ => effects += Effect("obs-err", "err"),
    ))

    effects shouldBe mutable.Buffer(Effect("obs", "start:0"))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("obs", "start:0 1"))
    effects.clear()

    // combine throws — signal enters error state
    bus.writer.onNext(-1)

    effects shouldBe mutable.Buffer(Effect("obs-err", "err"))
    effects.clear()

    // Error state persists
    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs-err", "err"))
  }

  it("Signal.scanLeft(resetOnStop=true): resets even when parent signal has not updated while stopped") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    val signal = v.signal.scanLeft(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = true,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    val sub = signal.addObserver(Observer[String](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", "start:1"))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "start:1 2"))
    effects.clear()

    sub.kill()

    // Parent signal does NOT update while stopped

    // Re-subscribe — even though parent hasn't changed, accumulated state is reset
    signal.addObserver(Observer[String](effects += Effect("obs", _)))

    // makeInitial(parent.now() = 2) = "start:2"
    effects shouldBe mutable.Buffer(Effect("obs", "start:2"))
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "start:2 3"))
  }

  it("EventStream.reduceLeftOption(resetOnStop=true): resets to None on restart; first event is new seed") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Option[Int]]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftOption[Int](resetOnStop = true)(_ + _)

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

  it("EventStream.reduceLeftOption(skipErrors=true): upstream errors are skipped; Option state is preserved") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Option[Int]]]()
    val bus = new EventBus[Int]

    val signal = bus.events.reduceLeftOption[Int](skipErrors = true)(_ + _)

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
