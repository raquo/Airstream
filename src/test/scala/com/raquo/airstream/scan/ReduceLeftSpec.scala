package com.raquo.airstream.scan

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class ReduceLeftSpec extends UnitSpec with BeforeAndAfter {

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

  it("Signal.reduceLeft uses the signal's initial value as the seed") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val v = Var(10)

    val signal = v.signal.reduceLeft[Int](_ + _)

    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(
      Effect("obs", 10)
    )

    effects.clear()

    // --

    v.writer.onNext(5)

    effects shouldBe mutable.Buffer(
      Effect("obs", 15)
    )

    effects.clear()

    // --

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(
      Effect("obs", 18)
    )
  }

  it("Signal.reduceLeft re-syncs with upstream when restarted") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val v = Var(1)

    val signal = v.signal.reduceLeft[Int](_ + _)

    val sub = signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", 3))
    effects.clear()

    sub.kill()

    // Upstream emits while stopped
    v.writer.onNext(10)

    // Re-subscribe — signal re-syncs with current upstream value
    signal.addObserver(Observer[Int](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 13))
    effects.clear()

    v.writer.onNext(4)

    effects shouldBe mutable.Buffer(Effect("obs", 17))
  }

  it("Signal.reduceLeft supports type widening") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Any]]()
    val v = Var(1)

    val signal: com.raquo.airstream.core.Signal[Any] =
      v.signal.reduceLeft[Any]((acc, next) => s"$acc+$next")

    signal.addObserver(Observer[Any](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", 1))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "1+2"))
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "1+2+3"))
  }

  it("Signal.scanLeft with constant initial value combines initial with parent's current value") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    // New overload: scanLeft(initial: => B)(fn) — initial is a constant seed,
    // but since Signal has an existing current value, the result's initial value is fn(seed, parent.now())
    val signal = v.signal.scanLeft("numbers:") { (acc, next) => acc + " " + next.toString }

    signal.addObserver(Observer[String](effects += Effect("obs", _)))

    // fn("numbers:", 1) => "numbers: 1"
    effects shouldBe mutable.Buffer(
      Effect("obs", "numbers: 1")
    )

    effects.clear()

    // --

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(
      Effect("obs", "numbers: 1 2")
    )

    effects.clear()

    // --

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(
      Effect("obs", "numbers: 1 2 3")
    )
  }

  it("Signal.scanLeft with constant initial re-syncs with upstream when restarted") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    val signal = v.signal.scanLeft("n:") { (acc, next) => acc + " " + next.toString }

    val sub = signal.addObserver(Observer[String](effects += Effect("obs", _)))

    // fn("n:", 1) = "n: 1"
    effects shouldBe mutable.Buffer(Effect("obs", "n: 1"))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "n: 1 2"))
    effects.clear()

    sub.kill()

    // Upstream emits while stopped
    v.writer.onNext(10)

    signal.addObserver(Observer[String](effects += Effect("obs", _)))

    // Re-syncs: the signal re-derives from upstream's new current value (10)
    // fn("n: 1 2", 10) = "n: 1 2 10"
    effects shouldBe mutable.Buffer(Effect("obs", "n: 1 2 10"))
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "n: 1 2 10 3"))
  }

  it("Signal.scanLeft constant initial differs from Signal.scanLeft makeInitial overload") {

    implicit val owner: TestableOwner = new TestableOwner

    val v = Var(5)

    // makeInitial overload: initial value of result = makeInitial(parent.now()) = parent.now() * 10 = 50
    val withMakeInitial = v.signal.scanLeft(makeInitial = (n: Int) => n * 10)(_ + _)

    // constant initial overload: initial value of result = fn(0, parent.now()) = 0 + 5 = 5
    val withConstantInitial = v.signal.scanLeft(0)(_ + _)

    withMakeInitial.addObserver(Observer.empty)
    withConstantInitial.addObserver(Observer.empty)

    withMakeInitial.now() shouldBe 50
    withConstantInitial.now() shouldBe 5
  }

  it("Signal.scanLeft with constant initial value: initial is evaluated once, when scanLeft is called") {

    implicit val owner: TestableOwner = new TestableOwner

    var initEvalCount = 0
    val v = Var(1)

    // initial: => B is passed to Try(initial) which forces evaluation in the scanLeft body,
    // because scanLeftRecover takes Try[B] strictly.
    val signal = v.signal.scanLeft {
      initEvalCount += 1
      "seed"
    } { (acc, next) => acc + next.toString }

    initEvalCount shouldBe 1 // eager: evaluated when scanLeft is called

    signal.addObserver(Observer.empty)
    initEvalCount shouldBe 1 // not re-evaluated on subscription

    v.writer.onNext(2)
    initEvalCount shouldBe 1 // not re-evaluated on updates
  }

  it("Signal.reduceLeftRecover handles errors in fn") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val v = Var(0)

    val signal = v.signal.reduceLeftRecover[Int] { (tryAcc, tryNext) =>
      for {
        acc  <- tryAcc
        next <- tryNext
      } yield {
        if (next < 0) throw err1
        acc + next
      }
    }

    val sub = signal.addObserver(Observer.withRecover[Int](
      effects += Effect("obs", _),
      err => effects += Effect("obs-err", err.getMessage.length) // length as a proxy for identity
    ))

    effects shouldBe mutable.Buffer(Effect("obs", 0))
    effects.clear()

    v.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs", 5))
    effects.clear()

    // Trigger error
    v.writer.onNext(-1)

    effects shouldBe mutable.Buffer(Effect("obs-err", err1.getMessage.length))
    effects.clear()

    // Recovery: next value uses the error in tryAcc — our fn handles it gracefully
    v.writer.onNext(3)

    // tryAcc is Failure(err1), tryNext is Success(3); the for-comprehension short-circuits on tryAcc failure
    signal.tryNow() shouldBe Failure(err1)
  }

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
}
