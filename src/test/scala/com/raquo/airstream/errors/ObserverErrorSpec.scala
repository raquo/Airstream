package com.raquo.airstream.errors

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError.{ObserverError, ObserverErrorHandlingError}
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, ExpectedError, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}


class ObserverErrorSpec extends UnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  val err0 = new Exception("err0")
  val err1 = new Exception("err1")
  val err2 = new Exception("err2")
  val err3 = new Exception("err3")
  val err31 = new Exception("err31")

  private val errorCallback = (err: Throwable) => {
    errorEffects += Effect("unhandled", err)
    ()
  }

  before {
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    errorEffects.clear()
    owner.killSubscriptions()
  }

  it("observer errors do not affect other observers") {

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val bus = new EventBus[Int]
    val signal = bus.events.startWith(0)

    val knownErrors = List(err0, err1, err2, err3)

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub1", _),
      { case err => errorEffects += Effect("sub1-err", err) }
    ))

    signal.addObserver(Observer.withRecover(
      num => if (num >= 0) throw err1 else throw err2,
      { case err if knownErrors.contains(err) => errorEffects += Effect("sub2-err", err) }
    ))

    signal.addObserver(Observer.withRecover(
      num => if (num % 2 == 0) effects += Effect("sub3", num) else throw err3,
      { case err if knownErrors.contains(err) => throw err31 }
    ))

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub4", _),
      { case err => errorEffects += Effect("sub4-err", err) }
    ))


    // Initial value triggers observer error

    assert(calculations.toList == Nil)
    assert(effects.toList == List(
      Effect("sub1", 0),
      Effect("sub3", 0),
      Effect("sub4", 0),
    ))
    assert(errorEffects.toList == List(
      Effect("unhandled", ObserverError(err1))
    ))

    effects.clear()
    errorEffects.clear()


    // Next value triggers an observer error where onError is also defined

    bus.writer.onNext(-1)

    assert(calculations.toList == Nil)
    assert(effects.toList == List(
      Effect("sub1", -1),
      Effect("sub4", -1),
    ))
    assert(errorEffects.toList == List(
      Effect("unhandled", ObserverError(err2)),
      Effect("unhandled", ObserverError(err3))
    ))

    effects.clear()
    errorEffects.clear()


    // Error value triggers an error in observer's onError

    bus.writer.onError(err0)

    assert(calculations.toList == Nil)
    assert(effects.toList == Nil)
    assert(errorEffects.toList == List(
      Effect("sub1-err", err0),
      Effect("sub2-err", err0),
      Effect("unhandled", ObserverErrorHandlingError(error = err31, cause = err0)),
      Effect("sub4-err", err0),
    ))

    errorEffects.clear()
  }

  it("contramapped observer passes errors upstream") {

    val effects = mutable.Buffer[Try[Int]]()

    val topObs = Observer.fromTry[Int] { case tryValue => effects += tryValue }

    val midObs = topObs.contramap[Int](_ + 100)

    val lowObs = midObs.contramap[Int](_ + 10)

    // --

    topObs.onError(err0)

    assert(effects.toList == List(Failure(err0)))

    effects.clear()

    // --

    lowObs.onError(err1)

    assert(effects.toList == List(Failure(err1)))

  }

  it("observer has a chance to handle its own error") {

    val divisionByZeroError = new Exception("divisionByZero")

    val effects = mutable.Buffer[Try[Int]]()

    val topObs = Observer.fromTry[Int] {
      case Success(0) => throw err0
      case Failure(ObserverError(`err0`)) => effects += Failure(divisionByZeroError)
      case tryValue => effects += tryValue
    }

    val lowObs = topObs.contramap[Int](_ + 100)

    // --

    topObs.onNext(1)

    assert(effects.toList == List(Success(1)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    topObs.onNext(0)

    assert(effects.toList == List(Failure(divisionByZeroError)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    topObs.onError(err0)

    assert(effects.toList == List(Failure(err0)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    topObs.onError(ObserverError(err0))

    assert(effects.toList == List(Failure(divisionByZeroError)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // -- Now check the contramapped observer behaviour

    lowObs.onNext(1)

    assert(effects.toList == List(Success(101)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    lowObs.onNext(-100) // This sends `0` to topObs

    assert(effects.toList == List(Failure(divisionByZeroError)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    lowObs.onError(err0)

    assert(effects.toList == List(Failure(err0)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    lowObs.onError(ObserverError(err0))

    assert(effects.toList == List(Failure(divisionByZeroError)))

    assert(errorEffects.toList == Nil)

    effects.clear()
  }

  it("observer can choose not to handle its own error") {

    val divisionByZeroError = new Exception("divisionByZero")

    val effects = mutable.Buffer[Try[Int]]()

    val topObs = Observer.fromTry[Int](
      {
        case Success(0) => throw err0
        case Failure(ObserverError(`err0`)) => effects += Failure(divisionByZeroError)
        case tryValue => effects += tryValue
      },
      handleObserverErrors = false
    )

    val lowObs = topObs.contramap[Int](_ + 100)

    // --

    topObs.onNext(1)

    assert(effects.toList == List(Success(1)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    topObs.onNext(0)

    assert(effects.toList == Nil)

    assert(errorEffects.toList == List(Effect("unhandled", ObserverError(err0))))

    errorEffects.clear()

    // --

    topObs.onError(err0)

    assert(effects.toList == List(Failure(err0)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    topObs.onError(ObserverError(err0))

    assert(effects.toList == List(Failure(divisionByZeroError)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // -- Now check the contramapped observer behaviour

    lowObs.onNext(1)

    assert(effects.toList == List(Success(101)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    lowObs.onNext(-100) // This sends `0` to topObs

    assert(effects.toList == Nil)

    assert(errorEffects.toList == List(Effect("unhandled", ObserverError(err0))))

    errorEffects.clear()

    // --

    lowObs.onError(err0)

    assert(effects.toList == List(Failure(err0)))

    assert(errorEffects.toList == Nil)

    effects.clear()

    // --

    lowObs.onError(ObserverError(err0))

    assert(effects.toList == List(Failure(divisionByZeroError)))

    assert(errorEffects.toList == Nil)

    effects.clear()
  }

  it("contracollectOpt emits value only if transformation returns a Some") {

    val effects = mutable.Buffer[Try[Int]]()

    val topObs = Observer.fromTry[Int] { case t => effects += t }

    val lowObs = topObs.contracollectOpt[Int](v => if (v != 2) Some(v) else None)

    lowObs.onNext(1)
    lowObs.onNext(2)
    lowObs.onNext(3)

    assert(effects.toList == List(Success(1), Success(3)))
  }

  it("contracollectOpt handles thrown exception from transformation ") {
    val effects = mutable.Buffer[Try[Int]]()

    val topObs = Observer.fromTry[Int] { case t => effects += t }

    val propagatedError = ExpectedError("propagated")
    val lowObs = topObs.contracollectOpt[Int](v => if (v == 2) throw ExpectedError("it's 2") else Some(v))

    lowObs.onNext(1)
    lowObs.onNext(2)
    lowObs.onNext(3)
    lowObs.onError(propagatedError)
    lowObs.onNext(5)

    assert(effects.toList == List(
      Success(1),
      Failure(ObserverError(ExpectedError("it's 2"))),
      Success(3),
      Failure(ExpectedError("propagated")),
      Success(5),
    ))
  }

}
