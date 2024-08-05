package com.raquo.airstream.extensions

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class EitherObservableSpec extends UnitSpec {

  it("EitherObservable: mapLeft, mapRight, mapEither") {

    implicit val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[Either[Int, String]]

    val effects = mutable.Buffer[Effect[_]]()
    bus.events
      .mapLeft(_ * 10)
      .mapRight(_ + "x")
      .foldEither(
        left = _.toString + "-left",
        right = _ + "-right"
      )
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Left(1))

    effects shouldBe mutable.Buffer(
      Effect("obs", "10-left")
    )
    effects.clear()

    // --

    bus.emit(Right("a"))

    effects shouldBe mutable.Buffer(
      Effect("obs", "ax-right")
    )
    effects.clear()

    // --

    bus.emit(Right("b"))

    effects shouldBe mutable.Buffer(
      Effect("obs", "bx-right")
    )
    effects.clear()
  }

  it("EitherStream: splitEither") {

    val owner: TestableOwner = new TestableOwner

    val innerOwner: TestableOwner = new TestableOwner

    val bus = new EventBus[Either[Int, String]]

    val effects = mutable.Buffer[Effect[_]]()

    var ix = 0
    bus.events
      .splitEither(
        left = (_, leftS) => {
          ix += 1
          leftS.foreach(l => effects += Effect(s"left-${ix}", l))(innerOwner)
          ix
        },
        right = (_, rightS) => {
          ix += 1
          rightS.foreach(r => effects += Effect(s"right-${ix}", r))(innerOwner)
          ix
        }
      )
      .foreach(v => effects += Effect("obs", v))(owner)

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Left(1))

    effects shouldBe mutable.Buffer(
      Effect("left-1", 1),
      Effect("obs", 1)
    )
    effects.clear()

    // --

    bus.emit(Left(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", 1),
      Effect("left-1", 2)
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to right

    bus.emit(Right("a"))

    effects shouldBe mutable.Buffer(
      Effect("right-2", "a"),
      Effect("obs", 2)
    )
    effects.clear()

    // --

    bus.emit(Right("b"))

    effects shouldBe mutable.Buffer(
      Effect("obs", 2),
      Effect("right-2", "b")
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to left

    bus.emit(Left(3))

    effects shouldBe mutable.Buffer(
      Effect("left-3", 3),
      Effect("obs", 3)
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to right

    bus.emit(Right("c"))

    effects shouldBe mutable.Buffer(
      Effect("right-4", "c"),
      Effect("obs", 4)
    )
    effects.clear()
  }

  it("EitherSignal: splitEither") {

    val owner: TestableOwner = new TestableOwner

    val innerOwner: TestableOwner = new TestableOwner

    val _var = Var[Either[Int, String]](Left(0))

    val effects = mutable.Buffer[Effect[_]]()

    var ix = 0
    _var.signal
      .splitEither(
        left = (_, leftS) => {
          ix += 1
          leftS.foreach(l => effects += Effect(s"left-${ix}", l))(innerOwner)
          ix
        },
        right = (_, rightS) => {
          ix += 1
          rightS.foreach(r => effects += Effect(s"right-${ix}", r))(innerOwner)
          ix
        }
      )
      .foreach(v => effects += Effect("obs", v))(owner)

    effects shouldBe mutable.Buffer(
      Effect("left-1", 0),
      Effect("obs", 1)
    )
    effects.clear()

    // --

    _var.set(Left(1))

    effects shouldBe mutable.Buffer(
      Effect("obs", 1),
      Effect("left-1", 1)
    )
    effects.clear()

    // --

    _var.set(Left(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", 1),
      Effect("left-1", 2)
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to right

    _var.set(Right("a"))

    effects shouldBe mutable.Buffer(
      Effect("right-2", "a"),
      Effect("obs", 2)
    )
    effects.clear()

    // --

    _var.set(Right("b"))

    effects shouldBe mutable.Buffer(
      Effect("obs", 2),
      Effect("right-2", "b")
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to left

    _var.set(Left(3))

    effects shouldBe mutable.Buffer(
      Effect("left-3", 3),
      Effect("obs", 3)
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions() // switch to right

    _var.set(Right("c"))

    effects shouldBe mutable.Buffer(
      Effect("right-4", "c"),
      Effect("obs", 4)
    )
    effects.clear()
  }

  it("EitherStream: collectRight") {
    val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[Either[Int, String]]

    val effects = mutable.Buffer[Effect[_]]()

    bus.events.collectRight
      .foreach(v => effects += Effect("obs", v))(owner)

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Left(1))

    effects.toList shouldBe Nil

    // --

    bus.emit(Right("a"))

    effects.toList shouldBe List(
      Effect("obs", "a")
    )
    effects.clear()

    // --

    bus.emit(Right("b"))

    effects.toList shouldBe List(
      Effect("obs", "b")
    )
    effects.clear()

    // --

    bus.emit(Left(2))

    effects.toList shouldBe Nil
  }

  it("EitherStream: collectRight()") {
    val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[Either[Int, String]]

    val effects = mutable.Buffer[Effect[_]]()

    bus.events
      .collectRight { case "a" => true }
      .foreach(v => effects += Effect("obs", v))(owner)

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Left(1))

    effects.toList shouldBe Nil

    // --

    bus.emit(Right("a"))

    effects.toList shouldBe List(
      Effect("obs", true)
    )
    effects.clear()

    // --

    bus.emit(Right("b"))

    effects.toList shouldBe Nil
    effects.clear()

    // --

    bus.emit(Right("a"))

    effects.toList shouldBe List(
      Effect("obs", true)
    )
    effects.clear()

    // --

    bus.emit(Left(2))

    effects.toList shouldBe Nil
  }
}
