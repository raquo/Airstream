package com.raquo.airstream.errors

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError.{CombinedError, ErrorHandlingError}
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.Failure

class EventStreamErrorSpec extends UnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val calculations = mutable.Buffer[Calculation[Int]]()
  private val effects = mutable.Buffer[Effect[Int]]()
  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  val err1 = new Exception("err1")
  val err2 = new Exception("err2")
  val err3 = new Exception("err3")

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
    calculations.clear()
    effects.clear()
    errorEffects.clear()
    owner.killSubscriptions()
  }

  it("map function is guarded against exceptions") {

    val stream = EventStream.fromSeq(List(-1, 2), emitOnce = true).map { num =>
      if (num < 0) throw err1 else num
    }.map(Calculation.log("stream", calculations))

    stream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", 2)
    )
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )
  }

  it("combined observable wraps errors") {

    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Int]

    val stream = bus1.events.combineWith(bus2.events).map2(_ * 100 + _).map(Calculation.log("stream", calculations))

    // sub1 does not handle errors, so they go to unhandled

    stream.addObserver(Observer(effects += Effect("sub", _)))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()

    bus1.writer.onError(err1)
    bus2.writer.onNext(1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("unhandled", CombinedError(List(Some(err1), None))),
    )

    errorEffects.clear()


    bus1.writer.onNext(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 101)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub", 101)
    )
    errorEffects shouldEqual mutable.Buffer()
  }

  it("event bus propagates errors to its stream and observer") {

    val bus = new EventBus[Int]

    val stream1 = bus.events.map(Calculation.log("stream1", calculations))

    // sub1 does not handle errors, so they go to unhandled

    stream1.addObserver(Observer(effects += Effect("sub1", _)))

    bus.writer.onError(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("unhandled", err1)
    )

    errorEffects.clear()


    // sub2 does handle errors, but sub1 is independent, so it still sends errors to unhandled

    stream1.addObserver(Observer.withRecover(
      effects += Effect("sub2", _),
      { case err => errorEffects += Effect("sub2-err", err) }
    ))

    bus.writer.onError(err2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(Effect("unhandled", err2), Effect("sub2-err", err2))

    errorEffects.clear()


    // Errors do not perma-break observables

    bus.writer.onNext(100)

    calculations shouldEqual mutable.Buffer(Calculation("stream1", 100))
    effects shouldEqual mutable.Buffer(Effect("sub1", 100), Effect("sub2", 100))
    errorEffects shouldEqual mutable.Buffer()
  }

  it("stream propagates errors to child streams and signals") {

    val bus = new EventBus[Int]

    val stream1 = bus.events.map(Calculation.log("stream1", calculations))
    val signal1 = stream1.toSignal(-1).map(Calculation.log("signal1", calculations))
    val signal2 = stream1.toSignal(-1).map(Calculation.log("signal2", calculations))

    // These subs do not handle errors, so they go to unhandled

    signal1.addObserver(Observer(effects += Effect("sub1Signal1", _)))
    signal2.addObserver(Observer(effects += Effect("sub1Signal2", _)))

    calculations shouldEqual mutable.Buffer(
      Calculation("signal1", -1),
      Calculation("signal2", -1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub1Signal1", -1),
      Effect("sub1Signal2", -1)
    )
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()

    bus.writer.onError(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      // Two errors because we have two observers failing to handle them
      Effect("unhandled", err1),
      Effect("unhandled", err1)
    )

    errorEffects.clear()


    // These signals do handle errors, but the initial ones are independent, so they still send errors to unhandled

    signal1.addObserver(Observer.withRecover(
      effects += Effect("sub2Signal1", _),
      { case err => errorEffects += Effect("sub2Signal1-err", err) }
    ))
    signal2.addObserver(Observer.withRecover(
      effects += Effect("sub2Signal2", _),
      { case err => errorEffects += Effect("sub2Signal2-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub2Signal1-err", err1),
      Effect("sub2Signal2-err", err1)
    )

    errorEffects.clear()

    bus.writer.onError(err2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("unhandled", err2),
      Effect("sub2Signal1-err", err2),
      Effect("unhandled", err2),
      Effect("sub2Signal2-err", err2)
    )

    errorEffects.clear()


    // Errors do not perma-break observables

    bus.writer.onNext(100)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream1", 100),
      Calculation("signal1", 100),
      Calculation("signal2", 100)
    )
    effects shouldEqual mutable.Buffer(
      Effect("sub1Signal1", 100),
      Effect("sub2Signal1", 100),
      Effect("sub1Signal2", 100),
      Effect("sub2Signal2", 100)
    )
    errorEffects shouldEqual mutable.Buffer()
  }

  it("stream recovers from errors") {

    val bus = new EventBus[Int]

    val errH = new Exception("errH")

    val upStream = bus.events.map(Calculation.log("upStream", calculations))
    val downStream = upStream.recover {
      case err if err == err1 => Some(1)
      case err if err == err2 => None
      case err if err == err3 => throw errH
    }.map(Calculation.log("downStream", calculations))

    downStream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))


    // Should recover from err1 into a value

    bus.writer.onError(err1)

    calculations shouldEqual mutable.Buffer(
      Calculation("downStream", 1)
    )
    effects shouldEqual mutable.Buffer(Effect("sub", 1))
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()


    // Should recover from err2 by skipping value

    bus.writer.onError(err2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()


    // Should fail to recover from err3 with a wrapped error

    bus.writer.onError(err3)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", ErrorHandlingError(error = errH, cause = err3))
    )
  }

  it("EventStream.fromTry") {

    val stream = EventStream.fromTry(Failure(err1), emitOnce = false).map(Calculation.log("stream", calculations))

    val sub = stream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      { case err => errorEffects += Effect("sub-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub-err", err1)
    )

    errorEffects.clear()
    sub.kill() // such a stream (emitOnce = false) re-emits only when it's started again, and for that it needs to become stopped first

    stream.addObserver(Observer.withRecover(
      effects += Effect("sub2", _),
      { case err => errorEffects += Effect("sub2-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("sub2-err", err1)
    )
  }

  it("Error that is not handled by `recover` is unhandled") {

    val stream = EventStream.fromTry(Failure(err1), emitOnce = true).map(Calculation.log("stream", calculations))

    stream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      // This only recovers from `err2, not `err1`
      { case err if err.getMessage == err2.getMessage => errorEffects += Effect("sub-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("unhandled", err1)
    )
  }
}
