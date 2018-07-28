package com.raquo.airstream.signal

import com.raquo.airstream.AsyncSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.mutable
import scala.concurrent.Promise

class AsyncSignalSpec extends AsyncSpec with BeforeAndAfter {

  describe("Signal.fromFuture") {

    implicit val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Option[Int]]]()
    val effects = mutable.Buffer[Effect[Option[Int]]]()

    val obs1 = Observer[Option[Int]](effects += Effect("obs1", _))
    val obs2 = Observer[Option[Int]](effects += Effect("obs2", _))

    def makePromise() = Promise[Int]()

    def assertEmptyLogs(): Assertion = {
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()
    }

    def clearLogs(): Assertion = {
      calculations.clear()
      effects.clear()
      assert(true)
    }

    def makeSignal(promise: Promise[Int]) = Signal
      .fromFuture(promise.future)
      .map(Calculation.log("signal", calculations))


    before {
      owner.killPossessions()
      clearLogs()
    }


    it("asynchronously emits a future value") {
      val promise = makePromise()
      val signal = makeSignal(promise)

      assertEmptyLogs()

      signal.addObserver(obs1)
      calculations shouldEqual mutable.Buffer(Calculation("signal", None))
      effects shouldEqual mutable.Buffer(Effect("obs1", None))
      clearLogs()

      delay {
        promise.success(100)
        assertEmptyLogs()

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
        assertEmptyLogs()
      }
    }

    it("synchronously emits if observers added asynchronously after the future resolves") {
      val promise = makePromise()
      val signal = makeSignal(promise)

      promise.success(100)

      delay {
        assertEmptyLogs()
        signal.addObserver(obs1)

        calculations shouldEqual mutable.Buffer(Calculation("signal", Some(100)))
        effects shouldEqual mutable.Buffer(Effect("obs1", Some(100)))
        clearLogs()

      }.flatMap { _ =>
        delay {
          assertEmptyLogs()
          signal.addObserver(obs2)

          effects shouldEqual mutable.Buffer(Effect("obs2", Some(100)))
          clearLogs()

        }.flatMap { _ =>
          delay {
            assertEmptyLogs()
          }
        }
      }
    }
  }
}
