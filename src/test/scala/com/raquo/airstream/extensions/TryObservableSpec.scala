package com.raquo.airstream.extensions

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class TryObservableSpec extends UnitSpec {

  case class TryError(msg: String) extends Throwable {
    override def getMessage: String = msg
  }

  it("TryObservable: mapSuccess, mapFailure, mapTry") {

    implicit val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[Try[Int]]

    val effects = mutable.Buffer[Effect[_]]()
    bus
      .events
      .mapFailure({
        case err @ TryError(msg) => err.copy(msg = msg + "-x")
        case other => other
      })
      .mapSuccess(_ * 10)
      .foldTry(
        success = _.toString + "-success",
        failure = _.getMessage + "-failure"
      )
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Failure(TryError("err")))

    effects shouldBe mutable.Buffer(
      Effect("obs", "err-x-failure")
    )
    effects.clear()

    // --

    bus.emit(Success(1))

    effects shouldBe mutable.Buffer(
      Effect("obs", "10-success")
    )
    effects.clear()

    // --

    bus.emit(Success(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", "20-success")
    )
    effects.clear()
  }

  it("TryStream: collectSuccess, collectFailure") {

    implicit val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[Try[Int]]

    val effects = mutable.Buffer[Effect[_]]()

    bus
      .events
      .collectSuccess { case x if x >= 10 => x.toString }
      .foreach(v => effects += Effect("obs-success", v))

    bus
      .events
      .collectFailure { case TryError(msg) => msg }
      .foreach(v => effects += Effect("obs-failure", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Failure(TryError("err")))

    effects shouldBe mutable.Buffer(
      Effect("obs-failure", "err")
    )
    effects.clear()

    // --

    bus.emit(Success(1))

    effects.shouldBeEmpty

    // --

    bus.emit(Success(20))

    effects shouldBe mutable.Buffer(
      Effect("obs-success", "20")
    )
    effects.clear()

    // --

    bus.emit(Failure(new Exception("...")))

    effects.shouldBeEmpty
  }

  it("TryStream: splitTry") {

    val owner: TestableOwner = new TestableOwner

    val innerOwner: TestableOwner = new TestableOwner

    val bus = new EventBus[Try[Int]]

    val effects = mutable.Buffer[Effect[_]]()

    var ix = 0
    bus
      .events
      .splitTry(
        success = (_, successS) => {
          ix += 1
          successS.foreach(r => effects += Effect(s"success-${ix}", r))(innerOwner)
          ix
        },
        failure = (_, failureS) => {
          ix += 1
          failureS.foreach(l => effects += Effect(s"failure-${ix}", l))(innerOwner)
          ix
        }
      )
      .foreach(v => effects += Effect("obs", v))(owner)

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Failure(TryError("err1")))

    effects shouldBe mutable.Buffer(
      Effect("failure-1", TryError("err1")),
      Effect("obs", 1)
    )
    effects.clear()

    // --

    bus.emit(Failure(TryError("err2")))

    effects shouldBe mutable.Buffer(
      Effect("obs", 1),
      Effect("failure-1", TryError("err2"))
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to right

    bus.emit(Success(100))

    effects shouldBe mutable.Buffer(
      Effect("success-2", 100),
      Effect("obs", 2),
    )
    effects.clear()

    // --

    bus.emit(Success(200))

    effects shouldBe mutable.Buffer(
      Effect("obs", 2),
      Effect("success-2", 200)
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to left

    bus.emit(Failure(TryError("err3")))

    effects shouldBe mutable.Buffer(
      Effect("failure-3", TryError("err3")),
      Effect("obs", 3),
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to right

    bus.emit(Success(400))

    effects shouldBe mutable.Buffer(
      Effect("success-4", 400),
      Effect("obs", 4)
    )
    effects.clear()

  }
}
