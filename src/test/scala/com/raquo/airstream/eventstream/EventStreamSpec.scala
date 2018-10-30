package com.raquo.airstream.eventstream

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable

class EventStreamSpec extends FunSpec with Matchers {

  it("filter works") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val bus = new EventBus[Int]
    val stream = bus.events.filter(f)

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = stream.foreach(newValue => effects += Effect("obs0", newValue))

    val range = 0 to 10
    range.foreach(bus.writer.onNext)
    effects.toList shouldBe range.filter(f).map(i => Effect("obs0", i))
    subscription0.kill()
  }

  it("filterNot works") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val bus = new EventBus[Int]
    val stream = bus.events.filterNot(f)

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = stream.foreach(newValue => effects += Effect("obs0", newValue))

    val range = 0 to 10
    range.foreach(bus.writer.onNext)
    effects.toList shouldBe range.filterNot(f).map(i => Effect("obs0", i))
    subscription0.kill()
  }

}
