package com.raquo.airstream.scan

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.Failure

class ScanLeftSignalSpec extends UnitSpec with BeforeAndAfter {

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
  // Basic signal creation
  // =========================================================================

  it("ScanLeftSignal made with EventStream.scanLeft") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer[String](effects += Effect("signal-obs", _))

    val bus = new EventBus[Int]

    val signal = bus.events
      .scanLeft(initial = "numbers:") { (acc, nextValue) => acc + " " + nextValue.toString }
      .map(Calculation.log("signal", calculations))

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers:")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers:")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 2")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 2")
    )

    calculations.clear()
    effects.clear()

    // --

    sub.kill()
    bus.writer.onNext(3)

    signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 2")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 2 4")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 2 4")
    )

    calculations.clear()
    effects.clear()

  }

  it("ScanLeftSignal made with Signal.scanLeft") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer[String](effects += Effect("signal-obs", _))

    val _var = Var(0)

    val signal = _var.signal
      .scanLeftGenerated(makeInitial = (initial: Int) => s"numbers: init=${initial}", resetOnStop = false, skipErrors = false) { (acc, nextValue) => acc + " " + nextValue.toString }
      .map(Calculation.log("signal", calculations))

    _var.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub1 = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1")
    )

    calculations.clear()
    effects.clear()

    // --

    _var.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2")
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    _var.writer.onNext(3)

    val sub2 = signal.addObserver(signalObserver)

    // Re-synced to upstream
    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3")
    )

    calculations.clear()
    effects.clear()

    // --

    _var.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3 4")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4")
    )

    calculations.clear()
    effects.clear()

    // --

    sub2.kill()

    val sub3 = signal.addObserver(signalObserver)

    // If $var does not emit while this signal is stopped, we don't need to re-sync with it.
    calculations.shouldBeEmpty
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4")
    )

    effects.clear()

    // --

    sub3.kill()

    _var.writer.onNext(4)

    signal.addObserver(signalObserver)

    // We detect that $var has emitted an event while this signal was stopped, and get $var's current value
    // We don't care that 4 == 4, we KNOW that $var has emitted by looking at lastUpdateId internally.
    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: init=1 2 3 4 4")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: init=1 2 3 4 4")
    )

    calculations.clear()
    effects.clear()
  }

  // =========================================================================
  // Basic Signal behavior
  // =========================================================================

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
    val withMakeInitial = v.signal.scanLeftGenerated(makeInitial = (n: Int) => n * 10, resetOnStop = false, skipErrors = false)(_ + _)

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

  // =========================================================================
  // Error handling
  // =========================================================================

  it("Signal.reduceLeftRecover handles errors in fn") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val v = Var(0)

    val signal = v.signal.reduceLeftRecover[Int] { (tryAcc, tryNext) =>
      for {
        acc <- tryAcc
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

  // =========================================================================
  // resetOnStop
  // =========================================================================

  it("Signal.scanLeft(makeInitial) resetOnStop: re-evaluates makeInitial vs re-syncs on restart; resets unconditionally") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    // -- resetOnStop=true re-evaluates makeInitial; resetOnStop=false re-syncs with parent --

    val signalReset = v.signal.scanLeftGenerated(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = true,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    val signalKeep = v.signal.scanLeftGenerated(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    val subReset = signalReset.addObserver(Observer[String](effects += Effect("reset", _)))
    val subKeep = signalKeep.addObserver(Observer[String](effects += Effect("keep", _)))

    effects shouldBe mutable.Buffer(Effect("reset", "start:1"), Effect("keep", "start:1"))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("reset", "start:1 2"), Effect("keep", "start:1 2"))
    effects.clear()

    subReset.kill()
    subKeep.kill()

    v.writer.onNext(10)

    signalReset.addObserver(Observer[String](effects += Effect("reset", _)))
    signalKeep.addObserver(Observer[String](effects += Effect("keep", _)))

    effects shouldBe mutable.Buffer(
      Effect("reset", "start:10"), // resetOnStop=true: re-evaluates makeInitial(parent.now=10)
      Effect("keep", "start:1 2 10"), // resetOnStop=false: re-syncs; combine("start:1 2", 10)
    )
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("reset", "start:10 3"), Effect("keep", "start:1 2 10 3"))
    effects.clear()

    // -- resetOnStop=true resets even when parent has not updated while stopped --

    val v2 = Var(1)
    val signalReset2 = v2.signal.scanLeftGenerated(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = true,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    val subReset2 = signalReset2.addObserver(Observer[String](effects += Effect("obs", _)))

    effects shouldBe mutable.Buffer(Effect("obs", "start:1"))
    effects.clear()

    v2.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs", "start:1 2"))
    effects.clear()

    subReset2.kill()

    // Parent signal does NOT update while stopped
    signalReset2.addObserver(Observer[String](effects += Effect("obs", _)))

    // Even though parent hasn't changed, accumulated state is still reset
    // makeInitial(parent.now() = 2) = "start:2"
    effects shouldBe mutable.Buffer(Effect("obs", "start:2"))
    effects.clear()

    v2.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs", "start:2 3"))
  }

  it("Signal.scanLeft(constant initial) resetOnStop: re-applies seed vs re-syncs on restart") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val v = Var(1)

    val signalReset = v.signal.scanLeft("n:", resetOnStop = true) { (acc, n) => s"$acc $n" }
    val signalKeep = v.signal.scanLeft("n:", resetOnStop = false) { (acc, n) => s"$acc $n" }

    val subReset = signalReset.addObserver(Observer[String](effects += Effect("reset", _)))
    val subKeep = signalKeep.addObserver(Observer[String](effects += Effect("keep", _)))

    // combine("n:", 1) = "n: 1"
    effects shouldBe mutable.Buffer(Effect("reset", "n: 1"), Effect("keep", "n: 1"))
    effects.clear()

    v.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("reset", "n: 1 2"), Effect("keep", "n: 1 2"))
    effects.clear()

    subReset.kill()
    subKeep.kill()

    v.writer.onNext(10)

    signalReset.addObserver(Observer[String](effects += Effect("reset", _)))
    signalKeep.addObserver(Observer[String](effects += Effect("keep", _)))

    effects shouldBe mutable.Buffer(
      Effect("reset", "n: 10"), // resetOnStop=true: re-apply seed to current parent → combine("n:", 10)
      Effect("keep", "n: 1 2 10"), // resetOnStop=false: re-sync; combine("n: 1 2", 10); seed is NOT re-applied
    )
    effects.clear()

    v.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("reset", "n: 10 3"), Effect("keep", "n: 1 2 10 3"))
  }

  // =========================================================================
  // skipErrors
  // =========================================================================

  it("Signal.scanLeft(makeInitial) skipErrors: upstream error — skipped vs propagated") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)

    val resultSkip = parentSignal.scanLeftGenerated(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = true,
    ) { (acc, n) => s"$acc $n" }

    val resultProp = parentSignal.scanLeftGenerated(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = false,
    ) { (acc, n) => s"$acc $n" }

    resultSkip.addObserver(Observer.withRecover[String](effects += Effect("skip", _), _ => effects += Effect("skip-err", "err")))
    resultProp.addObserver(Observer.withRecover[String](effects += Effect("prop", _), _ => effects += Effect("prop-err", "err")))

    // makeInitial(0) = "start:0"
    effects shouldBe mutable.Buffer(Effect("skip", "start:0"), Effect("prop", "start:0"))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("skip", "start:0 1"), Effect("prop", "start:0 1"))
    effects.clear()

    // Upstream error
    bus.writer.onError(err1)

    effects shouldBe mutable.Buffer(
      Effect("skip-err", "err"), // skipErrors=true: error is emitted
      Effect("prop-err", "err"), // skipErrors=false: error propagated
    )
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(
      Effect("skip", "start:0 1 2"), // skipErrors=true: accumulation continues from last non-error state
      Effect("prop-err", "err"), // skipErrors=false: error state persists
    )
  }

  it("Signal.scanLeft(makeInitial) skipErrors: combine error — skipped vs persists") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)

    val combine = (acc: String, n: Int) => { if (n < 0) throw err1; s"$acc $n" }

    val resultSkip = parentSignal.scanLeftGenerated(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = true,
    )(combine)

    val resultProp = parentSignal.scanLeftGenerated(
      makeInitial = (n: Int) => s"start:$n",
      resetOnStop = false,
      skipErrors = false,
    )(combine)

    resultSkip.addObserver(Observer.withRecover[String](effects += Effect("skip", _), _ => effects += Effect("skip-err", "err")))
    resultProp.addObserver(Observer.withRecover[String](effects += Effect("prop", _), _ => effects += Effect("prop-err", "err")))

    effects shouldBe mutable.Buffer(Effect("skip", "start:0"), Effect("prop", "start:0"))
    effects.clear()

    bus.writer.onNext(1)

    effects shouldBe mutable.Buffer(Effect("skip", "start:0 1"), Effect("prop", "start:0 1"))
    effects.clear()

    // combine throws for negative input
    bus.writer.onNext(-1)

    effects shouldBe mutable.Buffer(
      Effect("skip-err", "err"), // skipErrors=true: error is emitted
      Effect("prop-err", "err"), // skipErrors=false: error replaces state
    )
    errorEffects shouldBe mutable.Buffer()
    effects.clear()

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(
      Effect("skip", "start:0 1 2"), // skipErrors=true: continues from last non-error state
      Effect("prop-err", "err"), // skipErrors=false: error state persists
    )
  }
}
