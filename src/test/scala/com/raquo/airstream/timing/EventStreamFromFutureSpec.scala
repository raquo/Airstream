package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.mutable
import scala.concurrent.Promise

class EventStreamFromFutureSpec extends AsyncUnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val calculations = mutable.Buffer[Calculation[Int]]()
  private val effects = mutable.Buffer[Effect[Int]]()

  private val obs1 = Observer[Int](effects += Effect("obs1", _))
  private val obs2 = Observer[Int](effects += Effect("obs2", _))

  def makePromise(): Promise[Int] = Promise[Int]()

  def clearLogs(): Assertion = {
    calculations.clear()
    effects.clear()
    assert(true)
  }

  def makeStream(promise: Promise[Int]): EventStream[Int] = EventStream
    .fromFuture(promise.future)
    .map(Calculation.log("stream", calculations))


  before {
    owner.killSubscriptions()
    clearLogs()
  }


  it("asynchronously emits a future value") {
    val promise = makePromise()
    val stream = makeStream(promise)

    stream.addObserver(obs1)

    delay {
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()

      // --

      promise.success(100)

      // For consistency, the stream will only emit asynchronously even if the Future is already completed
      // This is how Future.onComplete behaves.
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldEqual mutable.Buffer(Calculation("stream", 100))
        effects shouldEqual mutable.Buffer(Effect("obs1", 100))
        clearLogs()
      }
    }
  }

  it("asynchronously emits if observer added synchronously right after the future resolves") {
    val promise = makePromise()
    val stream = makeStream(promise)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    promise.success(100)
    stream.addObserver(obs1)

    delay {
      calculations shouldEqual mutable.Buffer(Calculation("stream", 100))
      effects shouldEqual mutable.Buffer(Effect("obs1", 100))
      clearLogs()
    }
  }

  it("asynchronously emits if observer added asynchronously after the future resolves") {
    val promise = makePromise()
    val stream = makeStream(promise)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    promise.success(100)

    delay {
      stream.addObserver(obs1)
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldEqual mutable.Buffer(
          Calculation("stream", 100)
        )
        effects shouldEqual mutable.Buffer(
          Effect("obs1", 100)
        )
        clearLogs()
      }
    }
  }

  it("does not emit to second observer if it was added asynchronously after the future resolved") {
    val promise = makePromise()
    val stream = makeStream(promise)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    delay {
      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()

      stream.addObserver(obs1)
      promise.success(100)

      calculations shouldEqual mutable.Buffer()
      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldEqual mutable.Buffer(Calculation("stream", 100))
        effects shouldEqual mutable.Buffer(Effect("obs1", 100))
        clearLogs()

      }.flatMap { _ =>
        stream.addObserver(obs2)

        delay {
          calculations shouldEqual mutable.Buffer()
          effects shouldEqual mutable.Buffer()
        }
      }
    }
  }
}
