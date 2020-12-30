package com.raquo.airstream.eventstream

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.features.FlattenStrategy.{ConcurrentFutureStrategy, OverwriteFutureStrategy, SwitchFutureStrategy}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.Assertion

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

class EventStreamFlattenFutureSpec extends AsyncUnitSpec {

  it("EventStream.flatten(SwitchFutureStrategy)") {

    // @TODO[Test] Improve this test
    // We should better demonstrate the difference between this strategy and OverflowFutureFlattenStrategy
    // Basically, this strategy would fail the `promise5` part of overflow strategy's spec (see below)

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer[Int](effects += Effect("obs", _))

    def makePromise() = Promise[Int]()

    def clearLogs(): Assertion = {
      effects.clear()
      assert(true)
    }

    val promise1 = makePromise()
    val promise2 = makePromise()
    val promise3 = makePromise()
    val promise4 = makePromise()
    val promise5 = makePromise()

    val futureBus = new EventBus[Future[Int]]()
    val stream = futureBus.events.flatten(SwitchFutureStrategy)

    stream.addObserver(obs)

    futureBus.writer.onNext(promise1.future)
    futureBus.writer.onNext(promise2.future)

    delay {
      promise2.success(200)
      promise1.success(100)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 200))
      clearLogs()

      promise4.success(400)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer()

      futureBus.writer.onNext(promise3.future)
      futureBus.writer.onNext(promise4.future) // already resolved

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 400))
      clearLogs()

      promise3.success(300)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      futureBus.writer.onNext(promise5.future)
      promise5.success(500)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>

      effects shouldEqual mutable.Buffer(Effect("obs", 500))
      clearLogs()
    }
  }

  it("EventStream.flatten(ConcurrentFutureStrategy)") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer[Int](effects += Effect("obs", _))

    def makePromise() = Promise[Int]()

    def clearLogs(): Assertion = {
      effects.clear()
      assert(true)
    }

    val promise1 = makePromise()
    val promise2 = makePromise()
    val promise3 = makePromise()
    val promise4 = makePromise()
    val promise5 = makePromise()

    val futureBus = new EventBus[Future[Int]]()
    val stream = futureBus.events.flatten(ConcurrentFutureStrategy)

    stream.addObserver(obs)

    futureBus.writer.onNext(promise1.future)
    futureBus.writer.onNext(promise2.future)

    delay {
      promise2.success(200)
      promise1.success(100)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 200), Effect("obs", 100))
      clearLogs()

      promise4.success(400)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer()

      futureBus.writer.onNext(promise3.future)
      futureBus.writer.onNext(promise4.future) // already resolved
      futureBus.writer.onNext(promise5.future)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 400))
      clearLogs()

      promise3.success(300)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 300))
      clearLogs()
    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer()

      promise5.success(500)

      effects shouldEqual mutable.Buffer()
    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 500))
      clearLogs()
    }
  }

  it("EventStream.flatten(OverwriteFutureStrategy)") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer[Int](effects += Effect("obs", _))

    def makePromise() = Promise[Int]()

    def clearLogs(): Assertion = {
      effects.clear()
      assert(true)
    }

    val promise1 = makePromise()
    val promise2 = makePromise()
    val promise3 = makePromise()
    val promise4 = makePromise()
    val promise5 = makePromise()

    val futureBus = new EventBus[Future[Int]]()
    val stream: EventStream[Int] = futureBus.events.flatten(OverwriteFutureStrategy)

    stream.addObserver(obs)

    futureBus.writer.onNext(promise1.future)
    futureBus.writer.onNext(promise2.future)

    delay {
      promise2.success(200)
      promise1.success(100)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 200))
      clearLogs()

      promise4.success(400)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer()

      futureBus.writer.onNext(promise3.future)
      futureBus.writer.onNext(promise4.future) // already resolved
      futureBus.writer.onNext(promise5.future)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 400))
      clearLogs()

      promise3.success(300)

      effects shouldEqual mutable.Buffer()

    }.flatMap { _ =>
      promise5.success(500)

      effects shouldEqual mutable.Buffer()
    }.flatMap { _ =>
      effects shouldEqual mutable.Buffer(Effect("obs", 500))
      clearLogs()
    }
  }
}
