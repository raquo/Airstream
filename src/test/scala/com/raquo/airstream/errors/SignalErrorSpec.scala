package com.raquo.airstream.errors

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.{Val, Var}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class SignalErrorSpec extends UnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val calculations = mutable.Buffer[Calculation[Int]]()
  private val effects = mutable.Buffer[Effect[Int]]()
  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  private val errorCallback = (err: Throwable) => {
    errorEffects += Effect("unhandled", err)
    ()
  }

  val err1 = new Exception("err1")
  val err2 = new Exception("err2")
  val err3 = new Exception("err3")

  before {
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    calculations.clear()
    effects.clear()
    errorEffects.clear()
    owner.killSubscriptions()
  }

  it("initial value Success()") {

    val signalVar = Var[Int](1)
    val signal = signalVar.signal.map(Calculation.log("signal", calculations))

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))


    // Initial value should be evaluated and propagated to observer

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", 1)
    )
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()


    // Current value should be set to initial value

    signal.now() shouldEqual 1
    signal.tryNow() shouldEqual Success(1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()


    // Error value should propagate

    signalVar.writer.onError(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )

    errorEffects.clear()


    // Current value should be exposed as a Failure

    Try(signal.now()) shouldEqual Failure(err1)
    signal.tryNow() shouldEqual Failure(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()


    // Encountering the same error should trigger it again (because we didn't apply `distinctTry`)

    signalVar.writer.onError(err1)

    Try(signal.now()) shouldEqual Failure(err1)
    signal.tryNow() shouldEqual Failure(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )

    errorEffects.clear()
  }

  it("initial value Failure()") {

    val signalVar = Var.fromTry[Int](Failure(err1))
    val signal = signalVar.signal.map(Calculation.log("signal", calculations))

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))


    // Initial value should be evaluated and propagated to observer

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )

    errorEffects.clear()


    // Current error value should be set to initial value

    Try(signal.now()) shouldEqual Failure(err1)
    signal.tryNow() shouldEqual Failure(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()


    // Success value should propagate

    signalVar.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", 2)
    )
    errorEffects shouldEqual mutable.Buffer()
  }

  it("initial value Failure() when .changes is the only consumer") {

    val signalVar = Var.fromTry[Int](Failure(err1))
    val signal = signalVar.signal.map(Calculation.log("signal", calculations))
    val changes = signal.changes.map(Calculation.log("stream", calculations))

    changes.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))

    // Initial error value should not be evaluated (because no one is looking at it)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      //Effect("sub-err", err1)
    )

    // Success value should propagate

    signalVar.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 2),
      Calculation("stream", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", 2)
    )
    errorEffects shouldEqual mutable.Buffer()
  }

  it("map function is guarded against exceptions") {

    val signal = EventStream.fromSeq(List(1, -2, 3), emitOnce = true).map { num =>
      if (num < 0) throw err1 else num
    }.startWith(0).map(Calculation.log("signal", calculations))

    calculations shouldEqual mutable.Buffer()

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 0),
      Calculation("signal", 1),
      Calculation("signal", 3),
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", 0),
      Effect("sub", 1),
      Effect("sub", 3),
    )
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )
  }

  it("fold perma-breaks on error (note: use foldRecover to handle it)") {

    val bus = new EventBus[Int]

    val signalUp = bus.events.startWith(-1).foldLeft { num =>
      if (num < 0) {
        throw err1
      } else num
    }((acc, nextValue) => {
      if (nextValue == 10) throw err2 else acc + nextValue
    }).map(Calculation.log("signalUp", calculations))

    val signalDown = signalUp
      .recover { case _ => Some(-123) }
      .map(Calculation.log("signalDown", calculations))

    signalDown.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))


    // Error when calculating initial value should be recovered from

    calculations shouldEqual mutable.Buffer(
      Calculation("signalDown", -123)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", -123)
    )
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()


    // Current value is set to the initial error upstream but is recovered downstream

    signalUp.tryNow() shouldEqual Failure(err1)
    signalDown.tryNow() shouldEqual Success(-123)


    // Fold is now broken because it needs previous state, which it doesn't have. This is unlike other operators.

    bus.writer.onNext(1)

    signalUp.tryNow() shouldEqual Failure(err1)
    signalDown.tryNow() shouldEqual Success(-123)

    calculations shouldEqual mutable.Buffer(
      Calculation("signalDown",-123)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", -123)
    )
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()
  }


  it("foldRecover recovers from error") {

    val bus = new EventBus[Int]

    val signalUp = bus.events.startWith(-1).foldLeftRecover(tryNum => tryNum.map { num =>
      if (num < 0) {
        throw err1
      } else num
    })((tryAcc, tryNextValue) => {
      tryNextValue.map(tryAcc.getOrElse(-100) + _)
    }).map(Calculation.log("signalUp", calculations))

    val signalDown = signalUp
      .recover { case _ => Some(-123) }
      .map(Calculation.log("signalDown", calculations))

    signalDown.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))


    // Error when calculating initial value should be recovered from

    calculations shouldEqual mutable.Buffer(
      Calculation("signalDown", -123)
    )
    effects shouldEqual mutable.Buffer(Effect("sub", -123))
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()


    // Current value is set to the initial error upstream but is recovered downstream

    Try(signalUp.now()) shouldEqual Failure(err1)
    signalUp.tryNow() shouldEqual Failure(err1)

    Try(signalDown.now()) shouldEqual Success(-123)
    signalDown.tryNow() shouldEqual Success(-123)


    // foldRecover recovers from an error state

    bus.writer.onNext(1)

    signalUp.tryNow() shouldEqual Success(-99)
    signalDown.tryNow() shouldEqual Success(-99)

    calculations shouldEqual mutable.Buffer(
      Calculation("signalUp", -99),
      Calculation("signalDown", -99)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", -99)
    )
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()
  }

  it("flatMap propagates error in parent signal") {

    val owner = new TestableOwner

    val err = new Exception("No signal")

    val myVar = Var(0)

    // @TODO[Airstream] Add Signal.fromValue / fromTry that creates a Val
    val stream = myVar.signal.flatMap(Val(_))

    val effects = mutable.Buffer[Effect[_]]()

    stream.addObserver(Observer.withRecover(
      onNext = ev => effects += Effect("onNext", ev),
      onError = {
        case err => effects += Effect("onError", err.getMessage)
      }
    ))(owner)

    // -- initial value

    effects shouldBe mutable.Buffer(Effect("onNext", 0))
    effects.clear()

    // --

    myVar.set(1)

    effects shouldBe mutable.Buffer(Effect("onNext", 1))
    effects.clear()

    // --

    myVar.setError(err)
    effects shouldBe mutable.Buffer(Effect("onError", err.getMessage))
    effects.clear()

    // --

    myVar.set(2)

    effects shouldBe mutable.Buffer(Effect("onNext", 2))
    effects.clear()
  }

  it("flatMap propagates error in parent signal's initial value") {

    val owner = new TestableOwner

    val err = new Exception("No signal")

    val myVar = Var.fromTry[Int](Failure(err))

    // @TODO[Airstream] Add Signal.fromValue / fromTry that creates a Val
    val stream = myVar.signal.flatMap(Val(_))

    val effects = mutable.Buffer[Effect[_]]()

    stream.addObserver(Observer.withRecover(
      onNext = ev => effects += Effect("onNext", ev),
      onError = {
        case err => effects += Effect("onError", err.getMessage)
      }
    ))(owner)

    // -- initial value

    effects shouldBe mutable.Buffer(Effect("onError", err.getMessage))
    effects.clear()

    // --

    myVar.set(1)

    effects shouldBe mutable.Buffer(Effect("onNext", 1))
    effects.clear()

    // --

    myVar.setError(err)
    effects shouldBe mutable.Buffer(Effect("onError", err.getMessage))
    effects.clear()

    // --

    myVar.set(2)

    effects shouldBe mutable.Buffer(Effect("onNext", 2))
    effects.clear()
  }
}
