package com.raquo.airstream.util

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, TestableOwner}

import scala.collection.mutable

class RefSpec extends UnitSpec {

  case class Foo(id: Int)
  case class Bar(id: Int)

  it("Ref.eq") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[_]]()

    def log[V](name: String)(value: V): V = {
      val calculation = Calculation(name, value)
      // println(calculation)
      calculations += calculation
      value
    }

    val bus = new EventBus[Int]

    val signal = bus.events
      .startWith(initial = -1)
      .map(v => Ref.ref(Foo(v % 2)))
      .map(log("foo"))
      .map(v => Bar(v.value.id))
      .map(log("bar"))

    // --

    signal.observe
    bus.writer.onNext(1)

    // It's hard to test Refs because of their custom equality rules

    calculations.size shouldBe 4

    calculations(0).name shouldEqual "foo"
    calculations(0).value match {
      case ref: Ref[_] => ref.value shouldEqual Foo(-1)
      case _ => assert(false)
    }

    calculations(1).name shouldEqual "bar"
    calculations(1).value match {
      case Bar(v) => v shouldEqual -1
      case _ => assert(false)
    }

    calculations(2).name shouldEqual "foo"
    calculations(2).value match {
      case ref: Ref[_] => ref.value shouldEqual Foo(1)
      case _ => assert(false)
    }

    calculations(3).name shouldEqual "bar"
    calculations(3).value match {
      case Bar(v) => v shouldEqual 1
      case _ => assert(false)
    }

    calculations.clear()

    // --

    bus.writer.onNext(3)

    // Now, this next value will make it through Ref.eq[Foo], but will not make it through Bar.equals

    calculations.size shouldBe 1

    calculations(0).name shouldEqual "foo"
    calculations(0).value match {
      case ref: Ref[_] => ref.value shouldEqual Foo(1)
      case _ => assert(false)
    }
  }

}
