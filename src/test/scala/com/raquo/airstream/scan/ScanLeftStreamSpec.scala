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

    // No immediate change (this is a stream, not a signal)
    effects shouldBe mutable.Buffer()

    // The next event accumulates on top of the preserved state (30), NOT from 5 (which was lost)
    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(
      Effect("obs", 31) // 30 + 1, not 5 + 1 (5 was lost) and not 1 (state persisted)
    )
  }

  it("EventStream.scanLeft: accumulated state persists across stop/start; events missed while stopped are lost") {

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

  // =========================================================================
  // resetOnStop
  // =========================================================================

  it("EventStream.scanLeft resetOnStop: resets to seed vs preserves state on restart") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signalReset = bus.events.scanLeft(0, resetOnStop = true)(_ + _)
    val signalKeep = bus.events.scanLeft(0, resetOnStop = false)(_ + _)

    val subReset = signalReset.addObserver(Observer[Int](effects += Effect("reset", _)))
    val subKeep = signalKeep.addObserver(Observer[Int](effects += Effect("keep", _)))

    effects shouldBe mutable.Buffer(Effect("reset", 0), Effect("keep", 0))
    effects.clear()

    bus.writer.onNext(10)
    bus.writer.onNext(20)

    effects shouldBe mutable.Buffer(
      Effect("reset", 10), Effect("keep", 10),
      Effect("reset", 30), Effect("keep", 30),
    )
    effects.clear()

    subReset.kill()
    subKeep.kill()
    bus.writer.onNext(5) // missed while stopped

    signalReset.addObserver(Observer[Int](effects += Effect("reset", _)))
    signalKeep.addObserver(Observer[Int](effects += Effect("keep", _)))

    effects shouldBe mutable.Buffer(
      Effect("reset", 0), // resetOnStop=true: accumulated value reset to seed
      Effect("keep", 30), // resetOnStop=false: accumulated value preserved
    )
    effects.clear()

    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(
      Effect("reset", 3), // accumulates from reset seed (0)
      Effect("keep", 33), // accumulates from preserved state (30)
    )
  }

  // =========================================================================
  // skipErrors
  // =========================================================================

  it("EventStream.scanLeft skipErrors: upstream error — skipped vs propagated") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val signalSkip = bus.events.scanLeft(0, skipErrors = true)(_ + _)
    val signalProp = bus.events.scanLeft(0, skipErrors = false)(_ + _)

    signalSkip.addObserver(Observer.withRecover[Int](effects += Effect("skip", _), _ => effects += Effect("skip-err", -1)))
    signalProp.addObserver(Observer.withRecover[Int](effects += Effect("prop", _), _ => effects += Effect("prop-err", -1)))

    effects shouldBe mutable.Buffer(Effect("skip", 0), Effect("prop", 0))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("skip", 1), Effect("prop", 1))
    effects.clear()

    // Upstream error
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(
      Effect("skip-err", -1), // skipErrors=true: error is emitted
      Effect("prop-err", -1), // skipErrors=false: error propagated
    )
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(
      Effect("skip", 3), // skipErrors=true: continues from last non-error state (1)
      Effect("prop-err", -1), // skipErrors=false: error state persists
    )
    errorEffects shouldBe mutable.Buffer()
  }

  it("EventStream.scanLeft skipErrors: combine error — skipped vs persists") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val bus = new EventBus[Int]

    val combine = (acc: Int, next: Int) => { if (next < 0) throw err1; acc + next }

    val signalSkip = bus.events.scanLeft(0, skipErrors = true)(combine)
    val signalProp = bus.events.scanLeft(0, skipErrors = false)(combine)

    signalSkip.addObserver(Observer.withRecover[Int](effects += Effect("skip", _), _ => effects += Effect("skip-err", -1)))
    signalProp.addObserver(Observer.withRecover[Int](effects += Effect("prop", _), _ => effects += Effect("prop-err", -1)))

    effects shouldBe mutable.Buffer(Effect("skip", 0), Effect("prop", 0))
    effects.clear()

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("skip", 5), Effect("prop", 5))
    effects.clear()

    // combine throws for negative input
    bus.writer.onNext(-1)

    effects shouldBe mutable.Buffer(
      Effect("skip-err", -1), // skipErrors=true: error is emitted
      Effect("prop-err", -1), // skipErrors=false: error replaces state
    )
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    bus.writer.onNext(3)

    effects shouldBe mutable.Buffer(
      Effect("skip", 8), // skipErrors=true: continues from last non-error state (5)
      Effect("prop-err", -1), // skipErrors=false: error state persists
    )
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

    // Upstream error — emitted; last non-error state (5) preserved
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(
      Effect("obs", 5),
      Effect("obs-err", -1), // skipErrors=true: error is emitted
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
