package com.raquo.airstream.combine.generated

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.fixtures.TestableOwner

import scala.collection.mutable

class CombineEventStreamsSpec extends UnitSpec {

  case class T1(v: Int)
  case class T2(v: Int)
  case class T3(v: Int)
  case class T4(v: Int)
  case class T5(v: Int)
  case class T6(v: Int)
  case class T7(v: Int)
  case class T8(v: Int)
  case class T9(v: Int)

  it("CombineEventStream2 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events)

    val effects = mutable.Buffer[(T1, T2)]()

    val observer = Observer[(T1, T2)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1)),
          (T1(iteration), T2(iteration))
        )
      )
    }

    subscription.kill()
  }

  it("CombineEventStream3 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()
    val bus3 = new EventBus[T3]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events, bus3.events)

    val effects = mutable.Buffer[(T1, T2, T3)]()

    val observer = Observer[(T1, T2, T3)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldBe empty

    bus3.writer.onNext(T3(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0), T3(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      bus3.writer.onNext(T3(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1), T3(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration))
        )
      )
    }

    subscription.kill()
  }

  it("CombineEventStream4 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()
    val bus3 = new EventBus[T3]()
    val bus4 = new EventBus[T4]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events, bus3.events, bus4.events)

    val effects = mutable.Buffer[(T1, T2, T3, T4)]()

    val observer = Observer[(T1, T2, T3, T4)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldBe empty

    bus3.writer.onNext(T3(0))
    effects.toList shouldBe empty

    bus4.writer.onNext(T4(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0), T3(0), T4(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      bus3.writer.onNext(T3(iteration))
      bus4.writer.onNext(T4(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1), T3(iteration - 1), T4(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration - 1), T4(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration))
        )
      )
    }

    subscription.kill()
  }

  it("CombineEventStream5 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()
    val bus3 = new EventBus[T3]()
    val bus4 = new EventBus[T4]()
    val bus5 = new EventBus[T5]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events, bus3.events, bus4.events, bus5.events)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5)]()

    val observer = Observer[(T1, T2, T3, T4, T5)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldBe empty

    bus3.writer.onNext(T3(0))
    effects.toList shouldBe empty

    bus4.writer.onNext(T4(0))
    effects.toList shouldBe empty

    bus5.writer.onNext(T5(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0), T3(0), T4(0), T5(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      bus3.writer.onNext(T3(iteration))
      bus4.writer.onNext(T4(iteration))
      bus5.writer.onNext(T5(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration - 1), T5(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration))
        )
      )
    }

    subscription.kill()
  }

  it("CombineEventStream6 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()
    val bus3 = new EventBus[T3]()
    val bus4 = new EventBus[T4]()
    val bus5 = new EventBus[T5]()
    val bus6 = new EventBus[T6]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events, bus3.events, bus4.events, bus5.events, bus6.events)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldBe empty

    bus3.writer.onNext(T3(0))
    effects.toList shouldBe empty

    bus4.writer.onNext(T4(0))
    effects.toList shouldBe empty

    bus5.writer.onNext(T5(0))
    effects.toList shouldBe empty

    bus6.writer.onNext(T6(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0), T3(0), T4(0), T5(0), T6(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      bus3.writer.onNext(T3(iteration))
      bus4.writer.onNext(T4(iteration))
      bus5.writer.onNext(T5(iteration))
      bus6.writer.onNext(T6(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration - 1), T6(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration))
        )
      )
    }

    subscription.kill()
  }

  it("CombineEventStream7 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()
    val bus3 = new EventBus[T3]()
    val bus4 = new EventBus[T4]()
    val bus5 = new EventBus[T5]()
    val bus6 = new EventBus[T6]()
    val bus7 = new EventBus[T7]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events, bus3.events, bus4.events, bus5.events, bus6.events, bus7.events)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6, T7)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6, T7)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldBe empty

    bus3.writer.onNext(T3(0))
    effects.toList shouldBe empty

    bus4.writer.onNext(T4(0))
    effects.toList shouldBe empty

    bus5.writer.onNext(T5(0))
    effects.toList shouldBe empty

    bus6.writer.onNext(T6(0))
    effects.toList shouldBe empty

    bus7.writer.onNext(T7(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0), T3(0), T4(0), T5(0), T6(0), T7(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      bus3.writer.onNext(T3(iteration))
      bus4.writer.onNext(T4(iteration))
      bus5.writer.onNext(T5(iteration))
      bus6.writer.onNext(T6(iteration))
      bus7.writer.onNext(T7(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration - 1), T7(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration))
        )
      )
    }

    subscription.kill()
  }

  it("CombineEventStream8 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()
    val bus3 = new EventBus[T3]()
    val bus4 = new EventBus[T4]()
    val bus5 = new EventBus[T5]()
    val bus6 = new EventBus[T6]()
    val bus7 = new EventBus[T7]()
    val bus8 = new EventBus[T8]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events, bus3.events, bus4.events, bus5.events, bus6.events, bus7.events, bus8.events)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6, T7, T8)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6, T7, T8)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldBe empty

    bus3.writer.onNext(T3(0))
    effects.toList shouldBe empty

    bus4.writer.onNext(T4(0))
    effects.toList shouldBe empty

    bus5.writer.onNext(T5(0))
    effects.toList shouldBe empty

    bus6.writer.onNext(T6(0))
    effects.toList shouldBe empty

    bus7.writer.onNext(T7(0))
    effects.toList shouldBe empty

    bus8.writer.onNext(T8(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0), T3(0), T4(0), T5(0), T6(0), T7(0), T8(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      bus3.writer.onNext(T3(iteration))
      bus4.writer.onNext(T4(iteration))
      bus5.writer.onNext(T5(iteration))
      bus6.writer.onNext(T6(iteration))
      bus7.writer.onNext(T7(iteration))
      bus8.writer.onNext(T8(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration - 1), T8(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration), T8(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration), T8(iteration))
        )
      )
    }

    subscription.kill()
  }

  it("CombineEventStream9 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()
    val bus3 = new EventBus[T3]()
    val bus4 = new EventBus[T4]()
    val bus5 = new EventBus[T5]()
    val bus6 = new EventBus[T6]()
    val bus7 = new EventBus[T7]()
    val bus8 = new EventBus[T8]()
    val bus9 = new EventBus[T9]()

    val combinedStream = EventStream.combine(bus1.events, bus2.events, bus3.events, bus4.events, bus5.events, bus6.events, bus7.events, bus8.events, bus9.events)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6, T7, T8, T9)](effects += _)

    // --

    effects.toList shouldBe empty

    // --
    val subscription = combinedStream.addObserver(observer)
    effects.toList shouldBe empty

    // --

    bus1.writer.onNext(T1(0))
    effects.toList shouldBe empty

    bus2.writer.onNext(T2(0))
    effects.toList shouldBe empty

    bus3.writer.onNext(T3(0))
    effects.toList shouldBe empty

    bus4.writer.onNext(T4(0))
    effects.toList shouldBe empty

    bus5.writer.onNext(T5(0))
    effects.toList shouldBe empty

    bus6.writer.onNext(T6(0))
    effects.toList shouldBe empty

    bus7.writer.onNext(T7(0))
    effects.toList shouldBe empty

    bus8.writer.onNext(T8(0))
    effects.toList shouldBe empty

    bus9.writer.onNext(T9(0))
    effects.toList shouldEqual List(
      (T1(0), T2(0), T3(0), T4(0), T5(0), T6(0), T7(0), T8(0), T9(0))
    )

    // --
    for (iteration <- 1 to 10) {
      effects.clear()
      bus1.writer.onNext(T1(iteration))
      bus2.writer.onNext(T2(iteration))
      bus3.writer.onNext(T3(iteration))
      bus4.writer.onNext(T4(iteration))
      bus5.writer.onNext(T5(iteration))
      bus6.writer.onNext(T6(iteration))
      bus7.writer.onNext(T7(iteration))
      bus8.writer.onNext(T8(iteration))
      bus9.writer.onNext(T9(iteration))
      effects.toList should ===(
        List(
          (T1(iteration), T2(iteration - 1), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration - 1), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration - 1), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration - 1), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration - 1), T7(iteration - 1), T8(iteration - 1), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration - 1), T8(iteration - 1), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration), T8(iteration - 1), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration), T8(iteration), T9(iteration - 1)),
          (T1(iteration), T2(iteration), T3(iteration), T4(iteration), T5(iteration), T6(iteration), T7(iteration), T8(iteration), T9(iteration))
        )
      )
    }

    subscription.kill()
  }


}
