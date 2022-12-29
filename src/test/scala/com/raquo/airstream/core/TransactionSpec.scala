package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

/** A collection of tests that ensure that there are no FRP glitches */
class TransactionSpec extends UnitSpec {

  it("Nested transactions order and correctness") {

    val owner = new TestableOwner

    var n = 0
    val clickBus = new EventBus[Unit]
    val log = Var[List[Int]](Nil)

    clickBus
      .events
      .foreach { _ =>
        n = n + 2
        log.update(curr => {
          log.update(curr => curr :+ -1)
          log.update(curr => {
            log.update(curr => curr :+ -3)
            log.update(curr => curr :+ -4)
            curr :+ -2
          })
          log.update(curr => curr :+ -5)
          curr :+ (n - 2)
        })
        log.update(curr => curr :+ (n - 1))
      }(owner)

    clickBus.writer.onNext(())

    log.now() shouldBe List(0, -1, -2, -3, -4, -5, 1)

    // --

    clickBus.writer.onNext(())

    log.now() shouldBe List(0, -1, -2, -3, -4, -5, 1, 2, -1, -2, -3, -4, -5, 3)

    Transaction.isClearState shouldBe true
  }

  it("Errors in transaction code leave a recoverable state") {

    val unhandledErrors = mutable.Buffer[String]()

    val errorCallback = (err: Throwable) => {
      unhandledErrors.append(err.getMessage)
      ()
    }

    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)

    try {

      implicit val owner: TestableOwner = new TestableOwner

      val bus = new EventBus[Int]
      val log = Var[List[Int]](Nil)

      val effects = mutable.Buffer[Effect[Int]]()

      val obs1 = Observer[Int](effects += Effect("obs1", _))
      val obs2 = Observer[Int](effects += Effect("obs2", _))

      bus.events.addObserver(obs1)
      bus.events.map(_ * 10).addObserver(obs1)

      bus.events.foreach { num =>
        if (num % 2 == 0) {
          new Transaction(_ => {
            throw new Exception("Random error in transaction")
          })
        } else {
          log.update(_ :+ num)
        }
      }

      bus.events.addObserver(obs2)
      bus.events.map(_ * 10).addObserver(obs2)

      effects shouldBe mutable.Buffer()
      log.now() shouldBe Nil

      // --

      bus.writer.onNext(1)

      effects shouldBe mutable.Buffer(
        Effect("obs1", 1),
        Effect("obs2", 1),
        Effect("obs1", 10),
        Effect("obs2", 10)
      )
      effects.clear()

      log.now() shouldBe List(1)

      // --

      bus.writer.onNext(2)

      effects shouldBe mutable.Buffer(
        Effect("obs1", 2),
        Effect("obs2", 2),
        Effect("obs1", 20),
        Effect("obs2", 20)
      )
      effects.clear()

      log.now() shouldBe List(1)

      unhandledErrors.toList shouldBe List(
        "Random error in transaction"
      )
      unhandledErrors.clear()

      // --

      bus.writer.onNext(3)

      effects shouldBe mutable.Buffer(
        Effect("obs1", 3),
        Effect("obs2", 3),
        Effect("obs1", 30),
        Effect("obs2", 30)
      )
      effects.clear()

      log.now() shouldBe List(1, 3)

      // --

      bus.writer.onNext(4)

      effects shouldBe mutable.Buffer(
        Effect("obs1", 4),
        Effect("obs2", 4),
        Effect("obs1", 40),
        Effect("obs2", 40)
      )
      effects.clear()

      log.now() shouldBe List(1, 3)

      Transaction.isClearState shouldBe true

      unhandledErrors.toList shouldBe List(
        "Random error in transaction"
      )
      unhandledErrors.clear()

    } finally {
      AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
      AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
      assert(unhandledErrors.isEmpty)
    }
  }

}
