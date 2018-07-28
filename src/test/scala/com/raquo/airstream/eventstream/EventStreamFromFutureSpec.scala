package com.raquo.airstream.eventstream

import com.raquo.airstream.AsyncSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.mutable
import scala.concurrent.Promise

class EventStreamFromFutureSpec extends AsyncSpec with BeforeAndAfter {

  implicit val owner = new TestableOwner

  val calculations = mutable.Buffer[Calculation[Int]]()
  val effects = mutable.Buffer[Effect[Int]]()

  val obs1 = Observer[Int](effects += Effect("obs1", _))
  val obs2 = Observer[Int](effects += Effect("obs2", _))

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

  def makeStream(promise: Promise[Int]) = EventStream
    .fromFuture(promise.future)
    .map(Calculation.log("stream", calculations))


  before {
    owner.killPossessions()
    clearLogs()
  }


  it("asynchronously emits a future value") {
    val promise = makePromise()
    val stream = makeStream(promise)

    stream.addObserver(obs1)

    delay {
      assertEmptyLogs()

      // --

      promise.success(100)

      // For consistency, the stream will only emit asynchronously even if the Future is already completed
      // This is how Future.onComplete behaves.
      assertEmptyLogs()

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

    assertEmptyLogs()

    promise.success(100)
    stream.addObserver(obs1)

    delay {
      calculations shouldEqual mutable.Buffer(Calculation("stream", 100))
      effects shouldEqual mutable.Buffer(Effect("obs1", 100))
      clearLogs()
    }
  }

  it("does not emit if observer added asynchronously after the future resolves") {
    val promise = makePromise()
    val stream = makeStream(promise)

    assertEmptyLogs()

    promise.success(100)

    delay {
      assertEmptyLogs()
      stream.addObserver(obs1)

    }.flatMap { _ =>
      delay {
        assertEmptyLogs()
      }
    }
  }

  it("does not emit to second observer if it was added asynchronously after the future resolved") {
    val promise = makePromise()
    val stream = makeStream(promise)

    assertEmptyLogs()

    delay {
      assertEmptyLogs()

      stream.addObserver(obs1)
      promise.success(100)
      assertEmptyLogs()

    }.flatMap { _ =>
      delay {
        calculations shouldEqual mutable.Buffer(Calculation("stream", 100))
        effects shouldEqual mutable.Buffer(Effect("obs1", 100))
        clearLogs()

      }.flatMap { _ =>
        stream.addObserver(obs2)

        delay {
          assertEmptyLogs()
        }
      }
    }
  }
}
