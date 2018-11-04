package com.raquo.airstream.errors

import com.raquo.airstream.core.AirstreamError.{CombinedError, ErrorHandlingError}
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.mutable
import scala.util.Failure

class EventStreamErrorSpec extends FunSpec with Matchers with BeforeAndAfter {

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

  it("map function is guarded against exceptions") {

    val stream = EventStream.fromSeq(List(-1, 2)).map { num =>
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

  it("stream propagates errors to child streams, signals and state") {

    val bus = new EventBus[Int]

    val stream1 = bus.events.map(Calculation.log("stream1", calculations))
    val signal = stream1.toSignal(-1).map(Calculation.log("signal", calculations))
    val state = stream1.toState(-1).map(Calculation.log("state", calculations))

    // These subs do not handle errors, so they go to unhandled

    signal.addObserver(Observer(effects += Effect("subSignal1", _)))
    state.addObserver(Observer(effects += Effect("subState1", _)))

    // A bit non obvious: state is ahead of signal because it was added as internal observer when it was initialized,
    // which is earlier than when signal got its external observer
    calculations shouldEqual mutable.Buffer(
      Calculation("state", -1),
      Calculation("signal", -1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("subSignal1", -1), // @TODO[Integrity,Docs] But why are these in the opposite order?
      Effect("subState1", -1)
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


    // These observables do handle errors, but the initial ones are independent, so they still sends errors to unhandled

    signal.addObserver(Observer.withRecover(
      effects += Effect("subSignal2", _),
      { case err => errorEffects += Effect("subSignal2-err", err) }
    ))
    state.addObserver(Observer.withRecover(
      effects += Effect("subState2", _),
      { case err => errorEffects += Effect("subState2-err", err) }
    ))

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("subSignal2-err", err1),
      Effect("subState2-err", err1)
    )

    errorEffects.clear()

    bus.writer.onError(err2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("unhandled", err2),
      Effect("subState2-err", err2),
      Effect("unhandled", err2),
      Effect("subSignal2-err", err2)
    )

    errorEffects.clear()


    // Errors do not perma-break observables

    bus.writer.onNext(100)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream1", 100),
      Calculation("state", 100),
      Calculation("signal", 100)
    )
    effects shouldEqual mutable.Buffer(
      Effect("subState1", 100),
      Effect("subState2", 100),
      Effect("subSignal1", 100),
      Effect("subSignal2", 100)
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

    val stream = EventStream.fromTry(Failure(err1)).map(Calculation.log("stream", calculations))

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
    sub.kill() // such a stream re-emits only when it's started again, and for that it needs to become stopped first

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
}
