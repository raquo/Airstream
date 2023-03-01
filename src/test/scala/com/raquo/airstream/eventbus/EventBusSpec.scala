package com.raquo.airstream.eventbus

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class EventBusSpec extends UnitSpec {

  it("writer.onNext fires observers") {

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[Int]
    val effects = mutable.Buffer[Effect[_]]()

    bus.writer.onNext(1)

    val subscription0 = bus.events.foreach(newValue => effects += Effect("obs0", newValue))

    // new observer should not receive any previous events
    effects shouldBe mutable.Buffer()

    bus.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs0", 2))
    effects.clear()

    bus.emit(3)
    bus.emit(4)

    effects shouldBe mutable.Buffer(Effect("obs0", 3), Effect("obs0", 4))
    effects.clear()

    subscription0.kill()

    val obs1 = Observer[Int](newValue => effects += Effect("obs1", newValue))
    val obs2 = Observer[Int](newValue => effects += Effect("obs2", newValue))

    val sub1 = bus.events.addObserver(obs1)
    val sub2 = bus.events.addObserver(obs2)
    val subscription3 = bus.events.foreach(newValue => effects += Effect("obs3", newValue))

    bus.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs1", 5), Effect("obs2", 5), Effect("obs3", 5))
    effects.clear()

    sub2.kill()

    bus.writer.onNext(6)

    effects shouldBe mutable.Buffer(Effect("obs1", 6), Effect("obs3", 6))
    effects.clear()

    sub1.kill()

    bus.writer.onNext(7)
    bus.writer.onNext(8)

    effects shouldBe mutable.Buffer(Effect("obs3", 7), Effect("obs3", 8))
    effects.clear()

    subscription3.kill()

    bus.writer.onNext(9)

    effects shouldBe mutable.Buffer()
  }

  it("writer.addSource/removeSource fires observers") {

    val testOwner: TestableOwner = new TestableOwner
    val owner1: TestableOwner = new TestableOwner
    val owner2: TestableOwner = new TestableOwner

    val testBus = new EventBus[Int]
    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Int]

    val sourceStream1 = bus1.events.map(_ * 10)
    val sourceStream2 = bus2.events.map(_ * 100)

    val effects = mutable.Buffer[Effect[_]]()

    testBus.events.foreach(newValue => effects += Effect("obs0", newValue))(testOwner)

    bus1.writer.onNext(1)

    effects shouldBe mutable.Buffer()

    // ---

    val source1 = testBus.writer.addSource(sourceStream1)(owner1)
    bus1.writer.onNext(2)
    bus2.writer.onNext(2)

    effects shouldBe mutable.Buffer(Effect("obs0", 20))
    effects.clear()

    // ---

    testBus.writer.addSource(sourceStream2)(owner2)
    bus1.writer.onNext(3)
    bus2.writer.onNext(3)

    effects shouldBe mutable.Buffer(Effect("obs0", 30), Effect("obs0", 300))
    effects.clear()

    // ---

    source1.kill()

    bus1.writer.onNext(4)
    bus2.writer.onNext(4)

    effects shouldBe mutable.Buffer(Effect("obs0", 400))
    effects.clear()

    // --

    owner2.killSubscriptions()

    bus1.writer.onNext(5)
    bus2.writer.onNext(5)

    effects shouldBe mutable.Buffer()
  }

  it("disallow duplicate event buses in EventBus.emit and EventBus.emitTry") {
    // If we allowed this, you would be able to send two events into the same Var
    // in the same transaction, which breaks Airstream contract.
    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Int]
    val bus3 = new EventBus[String]

    // -- should not fail

    EventBus.emitTry(
      bus1 -> Success(1),
      bus2 -> Success(1),
      bus3 -> Failure(new Exception("Var 3 is broken"))
    )

    EventBus.emit(
      bus1 -> 1,
      bus2 -> 1,
      bus3 -> "1"
    )

    // --

    Try(EventBus.emit(
      bus1 -> 2,
      bus2 -> 2,
      bus1 -> 2
    )).isFailure shouldBe true

    // --

    Try(EventBus.emitTry(
      bus1 -> Success(3),
      bus2 -> Success(4),
      bus2 -> Success(5)
    )).isFailure shouldBe true
  }

  it("disallow duplicate event buses in WriteBus.emit and WriteBus.emitTry") {
    // If we allowed this, you would be able to send two events into the same Var
    // in the same transaction, which breaks Airstream contract.
    val bus1 = new EventBus[String]
    val bus2 = new EventBus[Int]
    val bus3 = new EventBus[Int]

    // -- should not fail

    WriteBus.emitTry(
      bus1.writer -> Success("1"),
      bus2.writer -> Success(1),
      bus3.writer -> Failure(new Exception("Var 3 is broken"))
    )

    WriteBus.emit(
      bus1.writer -> "1",
      bus2.writer -> 1,
      bus3.writer -> 1
    )

    // --

    Try(WriteBus.emit(
      bus1.writer -> "2",
      bus2.writer -> 2,
      bus1.writer -> "2"
    )).isFailure shouldBe true

    // --

    Try(WriteBus.emitTry(
      bus1.writer -> Success("3"),
      bus2.writer -> Success(4),
      bus2.writer -> Success(5)
    )).isFailure shouldBe true
  }
}
