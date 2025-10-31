package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.queue
import scala.scalajs.js.JSConverters.JSRichFutureNonThenable

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
    lazy val promise = makePromise()
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

  it("future and promise args are evaluated lazily") {
    var counter1 = 0
    var counter2 = 0
    var counter3 = 0
    var counter4 = 0

    def future1 = {
      counter1 += 1
      Future.successful(1)
    }

    def future2 = {
      counter2 += 1
      Future.successful(2)
    }

    def future3 = {
      counter3 += 1
      Future.successful(3)
    }

    def future4 = {
      counter4 += 1
      Future.successful(4)
    }

    val signal1 = Signal.fromFuture(future1)
    val signal2 = Signal.fromFuture(future2, initial = 0)
    val signal3 = Signal.fromJsPromise(future3.toJSPromise)
    val signal4 = Signal.fromJsPromise(future4.toJSPromise, initial = 0)

    val owner = new TestableOwner

    delay {
      assertEquals(counter1, 0)
      assertEquals(counter2, 0)
      assertEquals(counter3, 0)
      assertEquals(counter4, 0)

      // --

      val subs1 = List(
        signal1.foreach(_ => ())(owner),
        signal2.foreach(_ => ())(owner),
        signal3.foreach(_ => ())(owner),
        signal4.foreach(_ => ())(owner)
      )

      assertEquals(counter1, 1)
      assertEquals(counter2, 1)
      assertEquals(counter3, 1)
      assertEquals(counter4, 1)

      subs1.foreach(_.kill())

      // --

      val subs2 = List(
        signal1.foreach(_ => ())(owner),
        signal2.foreach(_ => ())(owner),
        signal3.foreach(_ => ())(owner),
        signal4.foreach(_ => ())(owner)
      )

      subs2.foreach(_.kill())

      // lazy val semantics #TODO[API] that's what we want, right?

      assertEquals(counter1, 1)
      assertEquals(counter2, 1)
      assertEquals(counter3, 1)
      assertEquals(counter4, 1)
    }
  }

}
