package com.raquo.airstream.extensions

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import scala.collection.mutable

class OptionObservableSpec extends UnitSpec {

  it("OptionObservable: mapSome") {

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[Option[Int]]

    val effects = mutable.Buffer[Effect[_]]()
    bus
      .events
      .mapSome(_ * 10)
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Some(1))

    effects shouldBe mutable.Buffer(
      Effect("obs", Some(10))
    )
    effects.clear()

    // --

    bus.emit(None)

    effects shouldBe mutable.Buffer(
      Effect("obs", None)
    )
    effects.clear()

    // --

    bus.emit(Some(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", Some(20))
    )
    effects.clear()

  }

  it("OptionStream: collectSome") {

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[Option[Int]]

    val effects = mutable.Buffer[Effect[_]]()
    bus
      .events
      .collectSome
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Some(1))

    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )
    effects.clear()

    // --

    bus.emit(Some(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )
    effects.clear()

    // --

    bus.emit(None)

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Some(3))

    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )
    effects.clear()

  }

  it("OptionStream: collectSome { ... }") {

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[Option[Int]]

    val effects = mutable.Buffer[Effect[_]]()
    bus
      .events
      .collectSome { case x if x % 2 == 0 => x }
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Some(1))

    effects.shouldBeEmpty

    // --

    bus.emit(Some(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )
    effects.clear()

    // --

    bus.emit(None)
    bus.emit(Some(3))

    effects shouldBe mutable.Buffer()
  }
}
