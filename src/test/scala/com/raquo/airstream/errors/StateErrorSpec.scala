package com.raquo.airstream.errors

import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.StateVar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class StateErrorSpec extends FunSpec with Matchers with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val calculations = mutable.Buffer[Calculation[Int]]()
  private val effects = mutable.Buffer[Effect[Int]]()
  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  val err1 = new Exception("err1")
  val err2 = new Exception("err2")
  val err3 = new Exception("err3")

  AirstreamError.registerUnhandledErrorCallback(errorEffects += Effect("unhandled", _))

  before {
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    calculations.clear()
    effects.clear()
    errorEffects.clear()
    owner.killPossessions()
  }

  it("initial value Success()") {

    val stateVar = StateVar[Int](1)
    val state = stateVar.state.map(Calculation.log("state", calculations))


    // Initial value should be evaluated

    state.now() shouldEqual 1
    state.tryNow() shouldEqual Success(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("state", 1)
    )
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()


    // Initial value should be propagated to observer

    state.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("sub", 1)
    )
    errorEffects shouldEqual mutable.Buffer()

    effects.clear()


    // Error value should propagate

    stateVar.writer.onError(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )

    errorEffects.clear()


    // Current value should be exposed as a Failure

    Try(state.now()) shouldEqual Failure(err1)
    state.tryNow() shouldEqual Failure(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()


    // Encountering the same error should not trigger it again (because value didn't change)

    stateVar.writer.onError(err1)

    Try(state.now()) shouldEqual Failure(err1)
    state.tryNow() shouldEqual Failure(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()
  }

  it("initial value Failure()") {

    val stateVar = StateVar.fromTry[Int](Failure(err1))
    val state = stateVar.state.map(Calculation.log("state", calculations))

    // Initial error value should be evaluated

    Try(state.now()) shouldEqual Failure(err1)
    state.tryNow() shouldEqual Failure(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()


    // Initial error should be propagated to observer

    state.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )

    errorEffects.clear()


    // Success value should propagate

    stateVar.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("state", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", 2)
    )
    errorEffects shouldEqual mutable.Buffer()
  }

  it("map function is guarded against exceptions") {

    EventStream.fromSeq(List(1, -2, 3)).map { num =>
      if (num < 0) throw err1 else num
    }.map(Calculation.log("stream", calculations)).toState(0).map(Calculation.log("state", calculations))

    // This behaviour is kinda undesired. We'd expect state to also evaluate Calculations for the initial event (0) and (1)
    // However, this does not happen because the stream emits the events when a State is first attached to it,
    // which is the .toState call. However, at that point the state that does the logging isn't actually created yet.
    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 1),
      Calculation("stream", 3),
      Calculation("state", 3),
    )
    effects shouldEqual mutable.Buffer() // No observer needed
    errorEffects shouldEqual mutable.Buffer( // No observer – send errors to unhandled
      Effect("unhandled", err1)
    )
  }

  it("map function is guarded against exceptions (proper event sequencing)") {

    val bus = new EventBus[Int]

    bus.events.map { num =>
      if (num < 0) throw err1 else num
    }.map(Calculation.log("stream", calculations)).toState(0).map(Calculation.log("state", calculations))

    bus.writer.onNext(1)
    bus.writer.onNext(-2)
    bus.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("state", 0),
      Calculation("stream", 1),
      Calculation("state", 1),
      Calculation("stream", 3),
      Calculation("state", 3),
    )
    effects shouldEqual mutable.Buffer() // No observer needed
    errorEffects shouldEqual mutable.Buffer( // No observer – send errors to unhandled
      Effect("unhandled", err1)
    )
  }

  it("fold perma-breaks on error (note: use foldRecover to handle it)") {

    val bus = new EventBus[Int]

    val stateUp = bus.events.toState(-1).fold { num =>
      if (num < 0) {
        throw err1
      } else num
    }((acc, nextValue) => {
      if (nextValue == 10) throw err2 else acc + nextValue
    }).map(Calculation.log("stateUp", calculations))

    val stateDown = stateUp
      .recover { case _ => Some(-123) }
      .map(Calculation.log("stateDown", calculations))

    stateDown.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))


    // Error when calculating initial value should be recovered from

    calculations shouldEqual mutable.Buffer(
      Calculation("stateDown", -123)
    )
    effects shouldEqual mutable.Buffer(Effect("sub", -123))
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()


    // Current value is set to the initial error upstream but is recovered downstream

    stateUp.tryNow() shouldEqual Failure(err1)
    stateDown.tryNow() shouldEqual Success(-123)


    // Fold is now broken because it needs previous state which it doesn't have. This is unlike other operators

    bus.writer.onNext(1)

    stateUp.tryNow() shouldEqual Failure(err1)
    stateDown.tryNow() shouldEqual Success(-123)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()
  }


  it("foldRecover recovers from error") {

    val bus = new EventBus[Int]

    val stateUp = bus.events.toState(-1).foldRecover(tryNum => tryNum.map { num =>
      if (num < 0) {
        throw err1
      } else num
    })((tryAcc, tryNextValue) => {
      tryNextValue.map(tryAcc.getOrElse(-100) + _)
    }).map(Calculation.log("stateUp", calculations))

    val stateDown = stateUp
      .recover { case _ => Some(-123) }
      .map(Calculation.log("stateDown", calculations))

    stateDown.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))


    // Error when calculating initial value should be recovered from

    calculations shouldEqual mutable.Buffer(
      Calculation("stateDown", -123)
    )
    effects shouldEqual mutable.Buffer(Effect("sub", -123))
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()


    // Current value is set to the initial error upstream but is recovered downstream

    Try(stateUp.now()) shouldEqual Failure(err1)
    stateUp.tryNow() shouldEqual Failure(err1)

    Try(stateDown.now()) shouldEqual Success(-123)
    stateDown.tryNow() shouldEqual Success(-123)


    // foldRecover recovers from an error state

    bus.writer.onNext(1)

    stateUp.tryNow() shouldEqual Success(-99)
    stateDown.tryNow() shouldEqual Success(-99)

    calculations shouldEqual mutable.Buffer(
      Calculation("stateUp", -99),
      Calculation("stateDown", -99)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", -99)
    )
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()
  }

  it("error in initial value is not reported as unhandled") {

    // Normally we'd expect this error to be reported as unhandled, ut it's not possible.
    // This is because initial value is evaluated immediately on state creation,
    // and so at this point state can't possibly have any observers, even if
    // its definition is followed immediately by .addObserver

    // We could address this by changing state API to evaluate initial value lazily, but that goes against its principles

    val state = StateVar.fromTry[Int](Failure(err1)).state.map(Calculation.log("state", calculations))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()

    errorEffects.clear()

    state.tryNow() shouldEqual Failure(err1) // Error is visible from here

    state.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer( // No observer – send errors to unhandled
      Effect("sub-err", err1)
    )
  }

}
