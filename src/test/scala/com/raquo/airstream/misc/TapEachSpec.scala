package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class TapEachSpec extends UnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val effects = mutable.Buffer[Effect[Int]]()
  private val tapEffects = mutable.Buffer[Effect[Int]]()
  private val tapErrorEffects = mutable.Buffer[Effect[Throwable]]()
  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  val err1 = new Exception("err1")
  val err2 = new Exception("err2")

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
    effects.clear()
    tapEffects.clear()
    tapErrorEffects.clear()
    errorEffects.clear()
    owner.killSubscriptions()
  }

  it("tapEach runs callback for every event on a stream") {

    val bus = new EventBus[Int]

    val stream = bus.events.tapEach { v => tapEffects += Effect("tap", v) }

    // Laziness: the callback does not run until there is an observer

    bus.writer.onNext(1)

    tapEffects shouldBe mutable.Buffer()

    stream.addObserver(Observer(effects += Effect("sub", _)))

    bus.writer.onNext(2)
    bus.writer.onNext(3)

    tapEffects shouldBe mutable.Buffer(
      Effect("tap", 2),
      Effect("tap", 3)
    )
    // tapEach passes the original value through unchanged
    effects shouldBe mutable.Buffer(
      Effect("sub", 2),
      Effect("sub", 3)
    )
    errorEffects shouldBe mutable.Buffer()
  }

  it("tapEach passes errors through without running the callback") {

    val bus = new EventBus[Int]

    val stream = bus.events.tapEach { v => tapEffects += Effect("tap", v) }

    stream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      err => errorEffects += Effect("sub-err", err)
    ))

    bus.writer.onError(err1)

    tapEffects shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer(
      Effect("sub-err", err1)
    )
  }

  it("tapEach exceptions are emitted as errors") {

    val bus = new EventBus[Int]

    val stream = bus.events.tapEach { v =>
      tapEffects += Effect("tap", v)
      if (v < 0) throw err1
    }

    stream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      err => errorEffects += Effect("sub-err", err)
    ))

    bus.writer.onNext(-1)
    bus.writer.onNext(2)

    tapEffects shouldBe mutable.Buffer(
      Effect("tap", -1),
      Effect("tap", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("sub", 2)
    )
    errorEffects shouldBe mutable.Buffer(
      Effect("sub-err", err1)
    )
  }

  it("tapEach runs callback on a signal's current value and updates") {

    val myVar = Var(1)

    val signal = myVar.signal.tapEach { v => tapEffects += Effect("tap", v) }

    signal.addObserver(Observer(effects += Effect("sub", _)))

    // Callback runs for the initial (current) value on subscription

    tapEffects shouldBe mutable.Buffer(
      Effect("tap", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("sub", 1)
    )

    myVar.set(2)
    myVar.set(3)

    tapEffects shouldBe mutable.Buffer(
      Effect("tap", 1),
      Effect("tap", 2),
      Effect("tap", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("sub", 1),
      Effect("sub", 2),
      Effect("sub", 3)
    )
  }

  it("tapEachError runs callback for every error on a stream, without affecting propagation") {

    val bus = new EventBus[Int]

    val stream = bus.events.tapEachError { err => tapErrorEffects += Effect("tap-err", err) }

    stream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      err => errorEffects += Effect("sub-err", err)
    ))

    // Values pass through untouched, and the callback is not run for them

    bus.writer.onNext(1)

    tapErrorEffects shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(Effect("sub", 1))
    errorEffects shouldBe mutable.Buffer()

    effects.clear()

    // Errors trigger the callback, but still propagate downstream unchanged

    bus.writer.onError(err1)
    bus.writer.onError(err2)

    tapErrorEffects shouldBe mutable.Buffer(
      Effect("tap-err", err1),
      Effect("tap-err", err2)
    )
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer(
      Effect("sub-err", err1),
      Effect("sub-err", err2)
    )
  }

  it("tapEachError callback runs exactly once per error") {

    val bus = new EventBus[Int]

    var callCount = 0

    val stream = bus.events.tapEachError { _ => callCount += 1 }

    stream.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      err => errorEffects += Effect("sub-err", err)
    ))

    bus.writer.onError(err1)

    callCount shouldBe 1
    errorEffects shouldBe mutable.Buffer(
      Effect("sub-err", err1)
    )
  }

  it("tapEachError runs callback for a signal's error updates") {

    val myVar = Var(1)

    val signal = myVar.signal.tapEachError { err => tapErrorEffects += Effect("tap-err", err) }

    signal.addObserver(Observer.withRecover(
      effects += Effect("sub", _),
      err => errorEffects += Effect("sub-err", err)
    ))

    tapErrorEffects shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(Effect("sub", 1))

    effects.clear()

    myVar.setError(err1)

    tapErrorEffects shouldBe mutable.Buffer(
      Effect("tap-err", err1)
    )
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer(
      Effect("sub-err", err1)
    )
  }
}
