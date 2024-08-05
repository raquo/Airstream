package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError.TransactionDepthExceeded
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var
import org.scalajs.dom
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

/** A collection of tests that ensure that there are no FRP glitches */
class TransactionSpec extends UnitSpec with BeforeAndAfter {

  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  private val errorCallback = (err: Throwable) => {
    errorEffects += Effect("unhandled", err)
    ()
  }

  before {
    errorEffects.clear()
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(
      AirstreamError.consoleErrorCallback
    )
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(
      AirstreamError.consoleErrorCallback
    )
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    assert(errorEffects.isEmpty) // #Note this fails the test rather inelegantly
  }

  it("Nested transactions order and correctness") {

    val owner = new TestableOwner

    var n = 0
    val clickBus = new EventBus[Unit]
    val log = Var[List[Int]](Nil)

    clickBus.events
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
        Transaction {
          throw new Exception("Random error in transaction")
        }
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

    errorEffects.map(_.value.getMessage).toList shouldBe List(
      "ObserverError: Exception: Random error in transaction"
    )
    errorEffects.clear()

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

    errorEffects.map(_.value.getMessage).toList shouldBe List(
      "ObserverError: Exception: Random error in transaction"
    )
    errorEffects.clear()
  }

  it("Stack safe (no overflow via breadth)") {
    val owner = new TestableOwner
    val bus = new EventBus[Unit]

    val num = 20000
    var ix = 0

    try {
      bus.events.foreach { _ =>
        new Transaction(_ =>
          for {
            i <- 1 to num
          } yield {
            new Transaction(_ => ix += 1)
          }
        )
      }(owner)
      bus.emit(())
      // dom.console.log(s"Done without errors: ${ix}")
    } catch {
      case err: Throwable =>
        dom.console.log(
          s"Stack overflow (depth) after ${ix} sibling transactions!"
        )
        throw err
    }
  }

  it("Stack safe (no overflow via depth)") {
    val owner = new TestableOwner
    val bus = new EventBus[Int]

    val defaultMaxDepth = Transaction.maxDepth

    val maxNum = 20000
    var ix = 0

    assert(
      defaultMaxDepth < maxNum,
      "defaultMaxDepth is weirdly high... all ok?"
    )

    Transaction.maxDepth = maxNum + 1

    try {
      bus.events
        .filter(_ < maxNum)
        .map { n =>
          ix = n + 1
          // dom.console.log(ix)
          ix
        }
        .foreach { n =>
          bus.emit(n)
        }(owner)
      bus.emit(0)
      // dom.console.log(s"Done without errors: ${ix}")
    } catch {
      case err: Throwable =>
        dom.console.log(
          s"Stack overflow (depth) after ${ix} nested transactions!"
        )
        throw err
    } finally {
      Transaction.maxDepth = defaultMaxDepth
    }
  }

  it("Exceeding max depth") {
    val owner = new TestableOwner
    val bus = new EventBus[Int]

    val maxNum = 20000
    var ix = 0

    var nestedIx = 0

    assert(
      Transaction.maxDepth < maxNum,
      "defaultMaxDepth is weirdly high... all ok?"
    )

    bus.events
      .filter(_ < maxNum)
      .map { n =>
        ix = n + 1
        // dom.console.log(ix)
        ix
      }
      .foreach { n =>
        Transaction { _ =>
          nestedIx += 1
          Transaction { _ =>
            nestedIx += 1
            Transaction { _ =>
              nestedIx += 1
              // dom.console.log(s"nestedIx=${nestedIx}")
            }
          }
        }
        bus.emit(n)
      }(owner)
    bus.emit(0)

    assert(errorEffects.nonEmpty)

    assert(!errorEffects.exists {
      case Effect("unhandled", TransactionDepthExceeded(_, _)) => false
      case _                                                   => true
    })

    errorEffects.clear()

    // dom.console.log(ix)
    // dom.console.log(nestedIx)

    assert(ix == Transaction.maxDepth)
    assert(nestedIx == 3 * (Transaction.maxDepth - 1) - 3)
  }
}
