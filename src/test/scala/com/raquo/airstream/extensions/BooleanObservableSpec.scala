package com.raquo.airstream.extensions

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class BooleanObservableSpec extends UnitSpec {

  it("BooleanObservable: invert") {

    implicit val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[Boolean]

    val effects = mutable.Buffer[Effect[_]]()
    bus
      .stream
      .invert
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(true)

    effects shouldBe mutable.Buffer(
      Effect("obs", false)
    )
    effects.clear()

    // --

    bus.emit(false)

    effects shouldBe mutable.Buffer(
      Effect("obs", true)
    )
    effects.clear()
  }

  it("BooleanStream: splitBoolean") {

    val owner: TestableOwner = new TestableOwner

    val innerOwner: TestableOwner = new TestableOwner

    val bus = new EventBus[Boolean]

    val effects = mutable.Buffer[Effect[_]]()
    bus
      .stream
      .splitBoolean(
        trueF = { signal =>
          effects += Effect("true", true)
          signal.foreach(v => effects += Effect("true-signal", v))(innerOwner)
          true
        },
        falseF = { signal =>
          effects += Effect("false", false)
          signal.foreach(v => effects += Effect("false-signal", v))(innerOwner)
          false
        }
      )
      .foreach(v => effects += Effect("obs", v))(owner)

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(true)

    effects shouldBe mutable.Buffer(
      Effect("true", true),
      Effect("true-signal", ()),
      Effect("obs", true)
    )
    effects.clear()

    // --

    bus.emit(true)

    effects shouldBe mutable.Buffer(
      Effect("obs", true),
      Effect("true-signal", ())
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions()

    bus.emit(false)

    effects shouldBe mutable.Buffer(
      Effect("false", false),
      Effect("false-signal", ()),
      Effect("obs", false)
    )
    effects.clear()

    // --

    bus.emit(false)

    effects shouldBe mutable.Buffer(
      Effect("obs", false),
      Effect("false-signal", ())
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions()

    bus.emit(true)

    effects shouldBe mutable.Buffer(
      Effect("true", true),
      Effect("true-signal", ()),
      Effect("obs", true)
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions()

    bus.emit(false)

    effects shouldBe mutable.Buffer(
      Effect("false", false),
      Effect("false-signal", ()),
      Effect("obs", false)
    )
    effects.clear()
  }

  it("BooleanSignal: splitBoolean") {

    val owner: TestableOwner = new TestableOwner

    val innerOwner: TestableOwner = new TestableOwner

    val _var = Var(true)

    val effects = mutable.Buffer[Effect[_]]()
    _var
      .signal
      .splitBoolean(
        whenTrue = { signal =>
          effects += Effect("true", true)
          signal.foreach(v => effects += Effect("true-signal", v))(innerOwner)
          true
        },
        whenFalse = { signal =>
          effects += Effect("false", false)
          signal.foreach(v => effects += Effect("false-signal", v))(innerOwner)
          false
        }
      )
      .foreach(v => effects += Effect("obs", v))(owner)

    effects shouldBe mutable.Buffer(
      Effect("true", true),
      Effect("true-signal", ()),
      Effect("obs", true)
    )
    effects.clear()

    // --

    _var.set(true)

    effects shouldBe mutable.Buffer(
      Effect("obs", true),
      Effect("true-signal", ())
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions()

    _var.set(false)

    effects shouldBe mutable.Buffer(
      Effect("false", false),
      Effect("false-signal", ()),
      Effect("obs", false)
    )
    effects.clear()

    // --

    _var.set(false)

    effects shouldBe mutable.Buffer(
      Effect("obs", false),
      Effect("false-signal", ())
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions()

    _var.set(true)

    effects shouldBe mutable.Buffer(
      Effect("true", true),
      Effect("true-signal", ()),
      Effect("obs", true)
    )
    effects.clear()

    // --

    innerOwner.killSubscriptions()

    _var.set(false)

    effects shouldBe mutable.Buffer(
      Effect("false", false),
      Effect("false-signal", ()),
      Effect("obs", false)
    )
    effects.clear()
  }
}
