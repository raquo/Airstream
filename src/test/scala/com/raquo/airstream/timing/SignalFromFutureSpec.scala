package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.mutable
import scala.concurrent.Promise

class SignalFromFutureSpec extends AsyncUnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val calculations = mutable.Buffer[Calculation[Option[Int]]]()
  private val effects = mutable.Buffer[Effect[Option[Int]]]()

  private val obs1 = Observer[Option[Int]](effects += Effect("obs1", _))
  private val obs2 = Observer[Option[Int]](effects += Effect("obs2", _))

  def makePromise(): Promise[Int] = Promise[Int]()

  def clearLogs(): Assertion = {
    calculations.clear()
    effects.clear()
    assert(true)
  }

  def makeSignal(promise: Promise[Int]): Signal[Option[Int]] = {
    Signal
      .fromFuture(promise.future)
      .map(Calculation.log("signal", calculations))
  }


  before {
    owner.killSubscriptions()
    clearLogs()
  }


  it("asynchronously emits a future value") {
    val promise = makePromise()
    val signal = makeSignal(promise)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    signal.addObserver(obs1)
    calculations shouldEqual mutable.Buffer(Calculation("signal", None))
    effects shouldEqual mutable.Buffer(Effect("obs1", None))
    clearLogs()

    delay {
      promise.success(100)
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldEqual mutable.Buffer(Calculation("signal", Some(100)))
        effects shouldEqual mutable.Buffer(Effect("obs1", Some(100)))
        clearLogs()
      }
    }
  }

  it("synchronously emits if observer added synchronously right after the future resolves") {
    val promise = makePromise()
    val signal = makeSignal(promise)

    promise.success(100)
    signal.addObserver(obs1)

    calculations shouldEqual mutable.Buffer(Calculation("signal", Some(100)))
    effects shouldEqual mutable.Buffer(Effect("obs1", Some(100)))
    clearLogs()

    delay {
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()
    }
  }

  it("synchronously emits if observers added asynchronously after the future resolves") {
    val promise = makePromise()
    val signal = makeSignal(promise)

    promise.success(100)

    delay {
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()
      signal.addObserver(obs1)

      calculations shouldEqual mutable.Buffer(Calculation("signal", Some(100)))
      effects shouldEqual mutable.Buffer(Effect("obs1", Some(100)))
      clearLogs()

    }.flatMap { _ =>
      delay {
        calculations shouldEqual mutable.Buffer()
        effects shouldEqual mutable.Buffer()
        signal.addObserver(obs2)

        effects shouldEqual mutable.Buffer(Effect("obs2", Some(100)))
        clearLogs()

      }.flatMap { _ =>
        delay {
          calculations shouldEqual mutable.Buffer()
          effects shouldEqual mutable.Buffer()
        }
      }
    }
  }

  it("exposes current value even without observers (unresolved future)") {
    val promise = makePromise()
    val signal = Signal.fromFuture(promise.future) // Don't use `makeSignal` here, we need the _original_, strict signal

    assert(signal.now().isEmpty)

    promise.success(100)

    // @TODO[API] This is empty because we've triggered signal's initialValue evaluation by the assert above.
    //  - After that, we can only track Future's updates asynchronously using onComplete.
    //  - I don't think we want to implement a pull-based system for this.
    assert(signal.now().isEmpty)

    delay {
      assert(signal.now().contains(100))
    }
  }

  it("exposes current value even without observers (resolved future)") {
    val promise = makePromise()
    promise.success(100)

    val signal = Signal.fromFuture(promise.future) // Don't use `makeSignal` here, we need the _original_, strict signal

    assert(signal.now().contains(100))
  }
}
