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

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    signal.addObserver(obs1)
    calculations shouldBe mutable.Buffer(Calculation("signal", None))
    effects shouldBe mutable.Buffer(Effect("obs1", None))
    clearLogs()

    delay {
      promise.success(100)
      calculations shouldBe mutable.Buffer()
      effects shouldBe mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldBe mutable.Buffer(Calculation("signal", Some(100)))
        effects shouldBe mutable.Buffer(Effect("obs1", Some(100)))
        clearLogs()
      }
    }
  }

  it("asynchronously emits if observer added synchronously right after the future resolves") {
    val promise = makePromise()
    val signal = makeSignal(promise)

    promise.success(100)
    signal.addObserver(obs1)

    calculations shouldBe mutable.Buffer(Calculation("signal", None))
    effects shouldBe mutable.Buffer(Effect("obs1", None))
    clearLogs()

    delay {
      calculations shouldBe mutable.Buffer(Calculation("signal", Some(100)))
      effects shouldBe mutable.Buffer(Effect("obs1", Some(100)))
      clearLogs()
    }
  }

  it("synchronously emits if observers added asynchronously after the future resolves and signal already emitted") {
    val promise = makePromise()
    val signal = makeSignal(promise)

    promise.success(100)

    delay {
      calculations shouldBe mutable.Buffer()
      effects shouldBe mutable.Buffer()
      signal.addObserver(obs1)

      calculations shouldBe mutable.Buffer(Calculation("signal", None))
      effects shouldBe mutable.Buffer(Effect("obs1", None))
      clearLogs()

    }.flatMap { _ =>
      delay {
        calculations shouldBe mutable.Buffer(Calculation("signal", Some(100)))
        effects shouldBe mutable.Buffer(Effect("obs1", Some(100)))
        clearLogs()

        signal.addObserver(obs2)

        effects shouldBe mutable.Buffer(Effect("obs2", Some(100)))
        clearLogs()

      }.flatMap { _ =>
        delay {
          calculations shouldBe mutable.Buffer()
          effects shouldBe mutable.Buffer()
        }
      }
    }
  }

}
