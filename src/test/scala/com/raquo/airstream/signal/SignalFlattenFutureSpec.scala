package com.raquo.airstream.signal

import com.raquo.airstream.AsyncSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.features.FlattenStrategy.{ConcurrentFutureStrategy, OverwriteFutureStrategy, SwitchFutureStrategy}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.Assertion

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

class SignalFlattenFutureSpec extends AsyncSpec {

  describe("Signal.flatten(SwitchFutureStrategy)") {

    it("initial unresolved future results in an async event") {

      implicit val owner = new TestableOwner

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
      val stream = futureBus.events.toSignal(promise0.future).flatten(SwitchFutureStrategy)

      stream.addObserver(obs)

      delay {
        promise0.success(-100)
        effects shouldEqual mutable.Buffer()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer(Effect("obs", -100))
        clearLogs()

        futureBus.writer.onNext(promise1.future)
        futureBus.writer.onNext(promise2.future)

        promise2.success(200)
        promise1.success(100)

        effects shouldEqual mutable.Buffer()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer(Effect("obs", 200))
        clearLogs()
      }
    }

    it("(1/2) initial already-resolved future does not result in an async event") {

      implicit val owner = new TestableOwner

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

      val stream = futureBus.events.toSignal(promise0.future).flatten(SwitchFutureStrategy)
      promise0.success(-100)

      delay {
        stream.addObserver(obs) // Adding observer asynchronously after both stream creation and future resolution

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer()

        futureBus.writer.onNext(promise1.future)
        futureBus.writer.onNext(promise2.future)

        promise2.success(200)
        promise1.success(100)

        effects shouldEqual mutable.Buffer()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer(Effect("obs", 200))
        clearLogs()
      }
    }

    it("(2/2) initial already-resolved future does not result in an async event (observer added sync with stream creation)") {

      implicit val owner = new TestableOwner

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
        val stream = futureBus.events.toSignal(promise0.future).flatten(SwitchFutureStrategy)
        stream.addObserver(obs)

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer()

        futureBus.writer.onNext(promise1.future)
        futureBus.writer.onNext(promise2.future)

        promise2.success(200)
        promise1.success(100)

        effects shouldEqual mutable.Buffer()

      }.flatMap { _ =>
        effects shouldEqual mutable.Buffer(Effect("obs", 200))
        clearLogs()
      }
    }
  }
}
