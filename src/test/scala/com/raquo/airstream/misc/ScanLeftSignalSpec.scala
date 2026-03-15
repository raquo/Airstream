package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.scan.ScanLeftSignal
import com.raquo.airstream.state.Var

import scala.collection.mutable
import scala.util.{Failure, Success}

class ScanLeftSignalSpec extends UnitSpec {

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
      .scanLeftGenerated(makeInitial = (initial: Int) => s"numbers: init=${initial}") { (acc, nextValue) => acc + " " + nextValue.toString }
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

  it("EventStream.scanLeft(resumeOnErrors = true)") {
    val err1 = new Exception("err1")
    val err2 = new Exception("err2")

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer.fromTry[String] {
      case Failure(err) =>
        effects += Effect("signal-obs", s"error: ${err.getMessage}")
      case Success(value) =>
        effects += Effect("signal-obs", value)
    }

    val bus = new EventBus[Int]

    val signal = bus.events
      .scanLeft(initial = "numbers:") { (acc, nextValue) =>
        if (nextValue >= 0) {
          acc + " " + nextValue.toString
        } else {
          throw new Exception(s"combineError${nextValue}")
        }
      }
      .map(Calculation.log("signal", calculations))

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

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 1")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 1")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onError(err1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: err1")
    )

    effects.clear()

    // --

    bus.writer.onError(err2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: err2")
    )

    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 1 2")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 1 2")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(-1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: combineError-1")
    )

    effects.clear()

    // --

    bus.writer.onNext(-2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: combineError-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 1 2 3")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 1 2 3")
    )

    calculations.clear()
    effects.clear()
  }

  it("Signal.scanLeft(resumeOnErrors = true)") {
    val err1 = new Exception("err1")
    val err2 = new Exception("err2")

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer.fromTry[String] {
      case Failure(err) =>
        effects += Effect("signal-obs", s"error: ${err.getMessage}")
      case Success(value) =>
        effects += Effect("signal-obs", value)
    }

    val bus = new EventBus[Int]

    val parentSignal = bus.events.startWith(0)

    val signal = parentSignal
      .scanLeftGenerated(makeInitial = (n: Int) => s"numbers: $n") { (acc, nextValue) =>
        if (nextValue >= 0) {
          acc + " " + nextValue.toString
        } else {
          throw new Exception(s"combineError${nextValue}")
        }
      }
      .map(Calculation.log("signal", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub1 = signal.addObserver(signalObserver)
    val parentSub = parentSignal.addObserver(Observer.fromTry(_ => ()))

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 0")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 0")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 0 1")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 0 1")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onError(err1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: err1")
    )

    effects.clear()

    // --

    bus.writer.onError(err2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: err2")
    )

    effects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 0 1 2")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 0 1 2")
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(-1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: combineError-1")
    )

    effects.clear()

    // --

    bus.writer.onNext(-2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: combineError-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 0 1 2 3")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 0 1 2 3")
    )

    calculations.clear()
    effects.clear()

    // Test the `currentValueFromParent` path below:

    // --

    sub1.kill()

    bus.writer.onNext(4)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub2 = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 0 1 2 3 4")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 0 1 2 3 4")
    )

    calculations.clear()
    effects.clear()

    // --

    sub2.kill()

    bus.writer.onError(err2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub3 = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: err2")
    )

    effects.clear()

    // --

    bus.writer.onNext(5)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", "numbers: 0 1 2 3 4 5")
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "numbers: 0 1 2 3 4 5")
    )
  }

  it("EventStream.scanLeft(resumeOnErrors = true) unrecoverable if initial value is error") {
    // Not like we WANT this, but just asserting that it does behave this way.
    // If we make the scanLeft initial argument by-name, we will expose ourselves to this scenario.

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer.fromTry[String] {
      case Failure(err) =>
        effects += Effect("signal-obs", s"error: ${err.getMessage}")
      case Success(value) =>
        effects += Effect("signal-obs", value)
    }

    val bus = new EventBus[Int]

    var initialIx = 0

    // Like scanLeft(initial)(...), but `initial` throws
    val signal = new ScanLeftSignal[Int, String, EventStream[Int]](
      parent = bus.events,
      makeInitialValue = () => {
        initialIx += 1
        throw new Exception(s"initialError${initialIx}")
      },
      fn = (accTry, nextTry) => {
        for {
          acc <- accTry
          nextValue <- nextTry
        } yield {
          if (nextValue >= 0) {
            acc + " " + nextValue.toString
          } else {
            throw new Exception(s"combineError${nextValue}")
          }
        }
      },
      resumeOnError = true
    )
      .map(Calculation.log("signal", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: initialError1")
    )

    effects.clear()

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: InitialValueError: Exception: initialError1")
    )

    effects.clear()

    // --

    bus.writer.onError(new Exception("err1"))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: InitialValueError: Exception: initialError1")
    )

    effects.clear()
  }

  it("Signal.scanLeft(resumeOnErrors = true) unrecoverable if initial value is error") {
    // Not like we WANT this, but just asserting that it does behave this way.
    // If we make the scanLeft initial argument by-name, we will expose ourselves to this scenario.

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[String]]()

    val signalObserver = Observer.fromTry[String] {
      case Failure(err) =>
        effects += Effect("signal-obs", s"error: ${err.getMessage}")
      case Success(value) =>
        effects += Effect("signal-obs", value)
    }

    val bus = new EventBus[Int]

    var initialIx = 0

    val parentSignal = bus.events.startWith(0)

    // Like scanLeft(initial)(...), but `initial` throws
    val signal = parentSignal
      .scanLeftGenerated(
        makeInitial = (n: Int) =>
          if (n == 0) {
            initialIx += 1
            throw new Exception(s"initialError${initialIx}")
          } else {
            s"numbers: $n"
          }
      ) { (acc, nextValue) =>
        if (nextValue >= 0) {
          acc + " " + nextValue.toString
        } else {
          throw new Exception(s"combineError${nextValue}")
        }
      }
      .map(Calculation.log("signal", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub1 = signal.addObserver(signalObserver)
    val observedParent = parentSignal.observe(testOwner)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: initialError1")
    )

    effects.clear()

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: InitialValueError: Exception: initialError1")
    )

    effects.clear()

    // --

    bus.writer.onError(new Exception("err1"))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: InitialValueError: Exception: initialError1")
    )

    effects.clear()

    // --

    // Test the `currentValueFromParent` path below:

    sub1.kill()

    bus.writer.onNext(2)

    observedParent.tryNow() shouldBe Success(2)

    val sub2 = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: InitialValueError: Exception: initialError1")
    )

    effects.clear()

    // --

    sub2.kill()

    bus.writer.onError(new Exception("err2"))

    observedParent.tryNow().isSuccess shouldBe false

    val sub3 = signal.addObserver(signalObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("signal-obs", "error: InitialValueError: Exception: initialError1")
    )

    effects.clear()
  }
}
