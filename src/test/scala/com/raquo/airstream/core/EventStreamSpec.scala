package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import scala.collection.mutable

class EventStreamSpec extends UnitSpec {

  it("filter") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val range = 0 to 10
    val stream = EventStream.fromSeq(range, emitOnce = true)

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = stream.filter(f).foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.filter(f).map(i => Effect("obs0", i))
  }

  it("filterNot") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val range = 0 to 10
    val stream = EventStream.fromSeq(range, emitOnce = true)

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = stream.filterNot(f).foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.filterNot(f).map(i => Effect("obs0", i))
  }

  // @TODO Enable this when we implement it
  ignore("distinct") {

    implicit val owner: Owner = new TestableOwner

    case class Foo(id: String, version: Int)

    val bus = new EventBus[Foo]

    val effects = mutable.Buffer[Effect[_]]()

    bus.events/**.distinct(_.id)*/.foreach { ev =>
      effects += Effect("obs", ev)
    }

    effects shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(Foo("bar", 1))

    effects shouldBe mutable.Buffer(Effect("obs", Foo("bar", 1)))
    effects.clear()

    // --

    bus.writer.onNext(Foo("bar", 2))

    effects shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(Foo("bar", 3))

    effects shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(Foo("baz", 1))

    effects shouldBe mutable.Buffer(Effect("obs", Foo("baz", 1)))
    effects.clear()

    // --

    bus.writer.onNext(Foo("baz", 2))

    effects shouldBe mutable.Buffer(Effect("obs", Foo("baz", 2)))
    effects.clear()

    // --

    bus.writer.onNext(Foo("bar", 4))

    effects shouldBe mutable.Buffer(Effect("obs", Foo("bar", 4)))
    effects.clear()

  }


}
