package com.raquo.airstream.flatten

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.Assertion

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

class SignalFlattenFutureSpec extends AsyncUnitSpec {

  describe("Signal.flatten") {

    it("initial unresolved future results in emitted default value and an async event") {

      implicit val owner: TestableOwner = new TestableOwner

      val effects = mutable.Buffer[Effect[Int]]()

      val obs = Observer[Int](effects += Effect("obs", _))

      def makePromise() = Promise[Int]()


      def clearLogs(): Assertion = {
        effects.clear()
        assert(true)
      }

      val promise0 = makePromise()
      val promise1 = makePromise()
      val promise2 = makePromise()

      val futureBus = new EventBus[Future[Int]]()
      val signal = futureBus.events
        .startWith(promise0.future)
        .flatMap(Signal.fromFuture(_, initial = -200))

      signal.addObserver(obs)

      delay {
        promise0.success(-100)
        effects shouldEqual mutable.Buffer(Effect("obs", -200))
        clearLogs()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer(Effect("obs", -100))
        clearLogs()

        futureBus.writer.onNext(promise1.future)
        futureBus.writer.onNext(promise2.future)

        promise2.success(200)

        promise1.success(100)

        // Since this is a Signal, and the futures were emitted prior to being resolved, we get their defined initial values
        effects shouldEqual mutable.Buffer(
          Effect("obs", -200),
          Effect("obs", -200)
        )
        clearLogs()

      }.flatMap { _ =>
        // Since the signal is only listening to the latest emitted future, we only get 200 here
        effects shouldEqual mutable.Buffer(Effect("obs", 200))
        clearLogs()
      }
    }

    it("initial future that is resolved sync-before the observer is added results in future's value used as signal's initial value") {

      implicit val owner: TestableOwner = new TestableOwner

      val effects = mutable.Buffer[Effect[Int]]()

      val obs = Observer[Int](effects += Effect("obs", _))

      def makePromise() = Promise[Int]()

      def clearLogs(): Assertion = {
        effects.clear()
        assert(true)
      }

      val promise0 = makePromise()
      val promise1 = makePromise()
      val promise2 = makePromise()

      val futureBus = new EventBus[Future[Int]]()

      val signal = futureBus.events.startWith(promise0.future).flatMap(Signal.fromFuture(_, initial = -200))
      promise0.success(-100)

      signal.addObserver(obs)

      effects shouldEqual mutable.Buffer(
        Effect("obs", -100)
      )
      clearLogs()

      delay {
        effects shouldEqual mutable.Buffer()

        futureBus.writer.onNext(promise1.future)
        futureBus.writer.onNext(promise2.future)

        promise2.success(200)
        promise1.success(100)

        // Emitting futures' initial values since they weren't resolved at the time of propagation
        effects shouldEqual mutable.Buffer(
          Effect("obs", -200),
          Effect("obs", -200)
        )
        effects.clear()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer(Effect("obs", 200))
        clearLogs()
      }
    }

    it("initial already-resolved future results in an async event if resolved async-before stream creation") {

      implicit val owner: TestableOwner = new TestableOwner

      val effects = mutable.Buffer[Effect[Int]]()

      val obs = Observer[Int](effects += Effect("obs", _))

      def makePromise() = Promise[Int]()

      def clearLogs(): Assertion = {
        effects.clear()
        assert(true)
      }

      val promise0 = makePromise()
      val promise1 = makePromise()
      val promise2 = makePromise()

      val futureBus = new EventBus[Future[Int]]()

      promise0.success(-100)

      delay {
        val signal = futureBus.events.startWith(promise0.future).flatMap(Signal.fromFuture(_, initial = -200))
        signal.addObserver(obs)

        effects shouldEqual mutable.Buffer(Effect("obs", -100))
        clearLogs()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer()

        futureBus.writer.onNext(promise1.future)
        futureBus.writer.onNext(promise2.future)

        promise2.success(200)
        promise1.success(100)

        // Emitting futures' initial values since they weren't resolved at the time of propagation
        effects shouldEqual mutable.Buffer(
          Effect("obs", -200),
          Effect("obs", -200)
        )
        clearLogs()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer(Effect("obs", 200))
        clearLogs()
      }
    }
  }
}
