package com.raquo.airstream.errors

import com.raquo.airstream.core.AirstreamError.{ObserverError, ObserverErrorHandlingError}
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.mutable


class ObserverErrorSpec extends FunSpec with Matchers with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val calculations = mutable.Buffer[Calculation[Int]]()
  private val effects = mutable.Buffer[Effect[Int]]()
  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  val err0 = new Exception("err0")
  val err1 = new Exception("err1")
  val err2 = new Exception("err2")
  val err3 = new Exception("err3")
  val err31 = new Exception("err31")

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

  it("observer errors do not affect other observers") {

    val bus = new EventBus[Int]
    val signal = bus.events.toSignal(0)

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub1", _),
      { case err => errorEffects += Effect("sub1-err", err) }
    ))

    signal.addObserver(Observer.withRecover(
      num => if (num >= 0) throw err1 else throw err2,
      { case err => errorEffects += Effect("sub2-err", err) }
    ))

    signal.addObserver(Observer.withRecover(
      num => if (num % 2 == 0) effects += Effect("sub3", num) else throw err3,
      { case err => throw err31 }
    ))

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub4", _),
      { case err => errorEffects += Effect("sub4-err", err) }
    ))


    // Initial value triggers observer error

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("sub1", 0),
      Effect("sub3", 0),
      Effect("sub4", 0),
    )
    errorEffects shouldEqual mutable.Buffer(
      Effect("unhandled", ObserverError(err1))
    )

    effects.clear()
    errorEffects.clear()


    // Next value triggers an observer error where onError is also defined

    bus.writer.onNext(-1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("sub1", -1),
      Effect("sub4", -1),
    )
    errorEffects shouldEqual mutable.Buffer(
      Effect("unhandled", ObserverError(err2)),
      Effect("unhandled", ObserverError(err3))
    )

    effects.clear()
    errorEffects.clear()


    // Error value triggers an error in observer's onError

    bus.writer.onError(err0)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub1-err", err0),
      Effect("sub2-err", err0),
      Effect("unhandled", ObserverErrorHandlingError(error = err31, cause = err0)),
      Effect("sub4-err", err0),
    )
  }
}
