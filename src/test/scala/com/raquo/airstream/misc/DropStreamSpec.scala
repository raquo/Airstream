package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}

import scala.collection.mutable

class DropStreamSpec extends UnitSpec {

  it("Drop first N events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .drop(numEvents = 2, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .drop(numEvents = 2, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 3),
      Effect("noreset", 4),
      Effect("reset", 3),
      Effect("reset", 4),
    )
    effects.clear()

    // --

    sub1.kill()
    sub2.kill()

    effects.toList shouldBe Nil

    // --

    val sub3 = $noreset.addObserver(Observer.empty)
    val sub4 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("noreset", 3),
      Effect("noreset", 4),
      Effect("reset", 3),
      Effect("reset", 4),
    )
    effects.clear()

  }

  it("Drop first ZERO events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .drop(numEvents = 0, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .drop(numEvents = 0, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("noreset", 3),
      Effect("noreset", 4),
      Effect("reset", 1),
      Effect("reset", 2),
      Effect("reset", 3),
      Effect("reset", 4),
    )
    effects.clear()

    // --

    sub1.kill()
    sub2.kill()

    effects.toList shouldBe Nil

    // --

    val sub3 = $noreset.addObserver(Observer.empty)
    val sub4 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("noreset", 3),
      Effect("noreset", 4),
      Effect("reset", 1),
      Effect("reset", 2),
      Effect("reset", 3),
      Effect("reset", 4),
    )
    effects.clear()

  }

  it("Drop while") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .dropWhile(_ <= 3, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .dropWhile(_ <= 3, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 4),
      Effect("noreset", 0),
      Effect("noreset", 5),
      Effect("reset", 4),
      Effect("reset", 0),
      Effect("reset", 5),
    )
    effects.clear()

    // --

    sub1.kill()
    sub2.kill()

    effects.toList shouldBe Nil

    // --

    val sub3 = $noreset.addObserver(Observer.empty)
    val sub4 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("noreset", 3),
      Effect("noreset", 4),
      Effect("noreset", 0),
      Effect("noreset", 5),
      Effect("reset", 4),
      Effect("reset", 0),
      Effect("reset", 5),
    )
    effects.clear()

  }

  it("Drop until") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .dropUntil(_ >= 4, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .dropUntil(_ >= 4, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 4),
      Effect("noreset", 0),
      Effect("noreset", 5),
      Effect("reset", 4),
      Effect("reset", 0),
      Effect("reset", 5),
    )
    effects.clear()

    // --

    sub1.kill()
    sub2.kill()

    effects.toList shouldBe Nil

    // --

    val sub3 = $noreset.addObserver(Observer.empty)
    val sub4 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("noreset", 3),
      Effect("noreset", 4),
      Effect("noreset", 0),
      Effect("noreset", 5),
      Effect("reset", 4),
      Effect("reset", 0),
      Effect("reset", 5),
    )
    effects.clear()

  }
}
