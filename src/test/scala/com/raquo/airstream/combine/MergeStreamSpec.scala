package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, TestableOwner}

import scala.collection.mutable

class MergeStreamSpec extends UnitSpec {

  it("order of events follows toporank") {

    val calculations = mutable.Buffer[Calculation[Int]]()

    val owner = new TestableOwner

    val bus = new EventBus[Int]
    val tens = bus.events.map(_ * 10)
    val hundreds = tens.map(_ * 10)

    val sub1 = EventStream.merge(tens, hundreds)
      .map(Calculation.log("merged", calculations))
      .addObserver(Observer.empty)(owner)

    // --

    bus.emit(1)

    calculations.toList shouldBe List(
      Calculation("merged", 10),
      Calculation("merged", 100),
    )

    calculations.clear()

    // --

    bus.emit(2)

    calculations.toList shouldBe List(
      Calculation("merged", 20),
      Calculation("merged", 200),
    )

    calculations.clear()

    // --

    sub1.kill()

    // -- Order of events is the same (based on topoRank) even if order of observables is reversed

    val sub2 = EventStream.merge(hundreds, tens)
      .map(Calculation.log("merged", calculations))
      .addObserver(Observer.empty)(owner)

    // --

    bus.emit(1)

    calculations.toList shouldBe List(
      Calculation("merged", 10),
      Calculation("merged", 100),
    )

    calculations.clear()
  }

  it("good behaviour when paired with combineWith") {

    // if combineWith's parent observable emits multiple events
    // in the same transaction (violating the transaction contract),
    // combineWith swallows all but the last event, so it is good
    // at exposing when parent observables emit more than once
    // in the same transaction.

    val calculations = mutable.Buffer[Calculation[Int]]()

    val owner = new TestableOwner

    val bus = new EventBus[Int]
    val tens = bus.events.map(_ * 10)
    val hundreds = tens.map(_ * 10)

    val sub1 = bus.events
      .combineWithFn(EventStream.merge(tens, hundreds))(_ + _)
      .map(Calculation.log("combined", calculations))
      .addObserver(Observer.empty)(owner)

    // --

    bus.emit(1)

    calculations.toList shouldBe List(
      Calculation("combined", 11),
      Calculation("combined", 101),
    )

    calculations.clear()

    // --

    bus.emit(2)

    calculations.toList shouldBe List(
      Calculation("combined", 22),
      Calculation("combined", 202),
    )

  }
}
