package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.queue
import scala.scalajs.js.JSConverters.JSRichFutureNonThenable

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
      calculations shouldBe mutable.Buffer()
      effects shouldBe mutable.Buffer()

      // --

      promise.success(100)

      // For consistency, the stream will only emit asynchronously even if the Future is already completed
      // This is how Future.onComplete behaves.
      calculations shouldBe mutable.Buffer()
      effects shouldBe mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldBe mutable.Buffer(Calculation("stream", 100))
        effects shouldBe mutable.Buffer(Effect("obs1", 100))
        clearLogs()
      }
    }
  }

  it("asynchronously emits if observer added synchronously right after the future resolves") {
    val promise = makePromise()
    val stream = makeStream(promise)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    promise.success(100)
    stream.addObserver(obs1)

    delay {
      calculations shouldBe mutable.Buffer(Calculation("stream", 100))
      effects shouldBe mutable.Buffer(Effect("obs1", 100))
      clearLogs()
    }
  }

  it("asynchronously emits if observer added asynchronously after the future resolves") {
    val promise = makePromise()
    val stream = makeStream(promise)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    promise.success(100)

    delay {
      stream.addObserver(obs1)
      calculations shouldBe mutable.Buffer()
      effects shouldBe mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldBe mutable.Buffer(
          Calculation("stream", 100)
        )
        effects shouldBe mutable.Buffer(
          Effect("obs1", 100)
        )
        clearLogs()
      }
    }
  }

  it("does not emit to second observer if it was added asynchronously after the future resolved") {
    val promise = makePromise()
    val stream = makeStream(promise)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    delay {
      calculations shouldBe mutable.Buffer()
      effects shouldBe mutable.Buffer()

      stream.addObserver(obs1)
      promise.success(100)

      calculations shouldBe mutable.Buffer()
      effects shouldBe mutable.Buffer()

    }.flatMap { _ =>
      delay {
        calculations shouldBe mutable.Buffer(Calculation("stream", 100))
        effects shouldBe mutable.Buffer(Effect("obs1", 100))
        clearLogs()

      }.flatMap { _ =>
        stream.addObserver(obs2)

        delay {
          calculations shouldBe mutable.Buffer()
          effects shouldBe mutable.Buffer()
        }
      }
    }
  }

  it("future and promise args are evaluated lazily") {
    var counter1 = 0
    var counter2 = 0

    def future1 = {
      counter1 += 1
      Future.successful(1)
    }

    def future2 = {
      counter2 += 1
      Future.successful(2)
    }

    val signal1 = EventStream.fromFuture(future1)
    val signal2 = EventStream.fromJsPromise(future2.toJSPromise)

    val owner = new TestableOwner

    delay {
      assertEquals(counter1, 0)
      assertEquals(counter2, 0)

      // --

      val subs1 = List(
        signal1.foreach(_ => ())(owner),
        signal2.foreach(_ => ())(owner),
      )

      assertEquals(counter1, 1)
      assertEquals(counter2, 1)

      subs1.foreach(_.kill())

      // --

      val subs2 = List(
        signal1.foreach(_ => ())(owner),
        signal2.foreach(_ => ())(owner),
      )

      subs2.foreach(_.kill())

      // lazy val semantics #TODO[API] that's what we want, right?

      assertEquals(counter1, 1)
      assertEquals(counter2, 1)
    }
  }
}
