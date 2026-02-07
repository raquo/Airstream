package com.raquo.airstream.extensions

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import scala.collection.mutable

class SeqObservableSpec extends UnitSpec {

  it("seqOrElse") {

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[List[Int]]

    val effects = mutable.Buffer[Effect[_]]()
    bus
      .events
      .seqOrElse(0)
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(List(1, 2, 3))

    effects shouldBe mutable.Buffer(
      Effect("obs", List(1, 2, 3))
    )
    effects.clear()

    // --

    bus.emit(Nil)

    effects shouldBe mutable.Buffer(
      Effect("obs", List(0))
    )
    effects.clear()

    // --

    bus.emit(List(4, 5, 6))

    effects shouldBe mutable.Buffer(
      Effect("obs", List(4, 5, 6))
    )
    effects.clear()

  }

  // trait Foo
  //
  // case class Bar(int: Int) extends Foo
  //
  // object NoFoo extends Foo

  // #nc[split] this needs a refactor of Splittable and MutableSplittable and their implicits
  //  - I'm not sure if it's possible to achieve...
  // it("seqOrElse with covariance") {
  //
  //   implicit val owner: Owner = new TestableOwner
  //
  //   val bus = new EventBus[List[Bar]]
  //
  //   val effects = mutable.Buffer[Effect[_]]()
  //   bus
  //     .events
  //     .seqOrElse(NoFoo) // #nc <<< this doesn't compile because we don't have AA >: A in seqOrElse because Splitaable's M is cont covariant because mutable types are supported but can't be covariant. Any chance we could maybe upcast M into something that is generic non-mutable in Scala? Or something...
  //     .foreach(v => effects += Effect("obs", v))
  //
  //   effects shouldBe mutable.Buffer()
  //
  //   // --
  //
  //   bus.emit(List(Bar(1), Bar(2)))
  //
  //   effects shouldBe mutable.Buffer(
  //     Effect("obs", Some(10))
  //   )
  //   effects.clear()
  //
  //   // --
  //
  //   bus.emit(None)
  //
  //   effects shouldBe mutable.Buffer(
  //     Effect("obs", None)
  //   )
  //   effects.clear()
  //
  //   // --
  //
  //   bus.emit(Some(2))
  //
  //   effects shouldBe mutable.Buffer(
  //     Effect("obs", Some(20))
  //   )
  //   effects.clear()
  //
  // }
  //
}
