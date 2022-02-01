package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}

import scala.collection.mutable

class TakeStreamSpec extends UnitSpec {

  it("Take first N events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .take(numEvents = 2, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .take(numEvents = 2, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("reset", 1),
      Effect("reset", 2),
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
      Effect("reset", 1),
      Effect("reset", 2),
    )
    effects.clear()

  }

  it("Take first ZERO events") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .take(numEvents = 0, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4))
      .take(numEvents = 0, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe Nil

    // --

    sub1.kill()
    sub2.kill()

    effects.toList shouldBe Nil

    // --

    val sub3 = $noreset.addObserver(Observer.empty)
    val sub4 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe Nil
    effects.clear()

  }

  it("Take while") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .takeWhile(_ <= 3, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .takeWhile(_ <= 3, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("noreset", 3),
      Effect("reset", 1),
      Effect("reset", 2),
      Effect("reset", 3),
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
      Effect("reset", 1),
      Effect("reset", 2),
      Effect("reset", 3),
    )
    effects.clear()

  }

  it("Take until") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val $noreset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .takeUntil(_ >= 4, resetOnStop = false)
      .map(Effect.log("noreset", effects))

    val $reset = EventStream
      .fromSeq(List(1, 2, 3, 4, 0, 5))
      .takeUntil(_ >= 4, resetOnStop = true)
      .map(Effect.log("reset", effects))

    // --

    val sub1 = $noreset.addObserver(Observer.empty)
    val sub2 = $reset.addObserver(Observer.empty)

    effects.toList shouldBe List(
      Effect("noreset", 1),
      Effect("noreset", 2),
      Effect("noreset", 3),
      Effect("reset", 1),
      Effect("reset", 2),
      Effect("reset", 3),
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
      Effect("reset", 1),
      Effect("reset", 2),
      Effect("reset", 3),
    )
    effects.clear()

  }
}
