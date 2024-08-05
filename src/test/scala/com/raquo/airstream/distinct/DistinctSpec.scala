package com.raquo.airstream.distinct

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable
import scala.util.{Failure, Success}

class DistinctSpec extends UnitSpec {

  it("distinct streams") {

    lazy val err1 = new Exception("err1")

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val errorEffects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val obs = Observer.fromTry[Int] {
      case Success(value) => effects += Effect("obs", value)
      case Failure(err)   => errorEffects += Effect("obs-err", err.getMessage)
    }

    val bus = new EventBus[Int]
    val stream = bus.events.distinct
      .map(Calculation.log("stream", calculations))

    val sub1 = stream.addObserver(obs)(testOwner)

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    val sub2 = stream.addObserver(obs)(testOwner)

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    errorEffects shouldBe mutable.Buffer() // nothing failed yet

    bus.writer.onError(err1)

    calculations shouldBe mutable.Buffer()

    effects shouldBe mutable.Buffer()

    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err1")
    )

    errorEffects.clear()

    // -- errors are not distincted by the `distinct` method

    bus.writer.onError(err1)

    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err1")
    )
  }

  it("distinct signals") {

    lazy val err1 = new Exception("err1")

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val errorEffects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val obs = Observer.fromTry[Int] {
      case Success(value) => effects += Effect("obs", value)
      case Failure(err)   => errorEffects += Effect("obs-err", err.getMessage)
    }

    val _var = Var(0)
    val signal = _var.signal.distinct
      .map(Calculation.log("signal", calculations))

    val sub1 = signal.addObserver(obs)(testOwner)

    // --

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 0)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 0)
    )

    calculations.clear()
    effects.clear()

    // --

    _var.writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    _var.writer.onNext(1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    _var.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )

    calculations.clear()
    effects.clear()

    // -- don't pull new value if parent has not emitted new values

    sub1.kill()

    val sub2 = signal.addObserver(obs)(testOwner)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )
    calculations.clear()
    effects.clear()

    // --

    sub2.kill()

    _var.writer.onNext(2)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // -- don't emit new value if parent has emitted and its new value is isSame(lastParentValue)

    val sub3 = signal.addObserver(obs)(testOwner)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )
    calculations.clear()
    effects.clear()

    // --

    sub3.kill()

    _var.writer.onNext(3)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // -- emit new value if parent has emitted and its new value is NOT isSame(lastParentValue)

    val sub4 = signal.addObserver(obs)(testOwner)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )
    calculations.clear()
    effects.clear()

    // --

    _var.writer.onNext(3)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    _var.writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 4)
    )

    calculations.clear()
    effects.clear()

    // --

    errorEffects shouldBe mutable.Buffer() // nothing failed yet

    _var.writer.onError(err1)

    calculations shouldBe mutable.Buffer()

    effects shouldBe mutable.Buffer()

    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err1")
    )

    errorEffects.clear()

    // -- errors are not distincted by the `distinct` method

    _var.writer.onError(err1)

    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err1")
    )
  }

  it("distinct errors") {

    lazy val err1 = new Exception("err1")
    lazy val err2 = new Exception("err2")
    lazy val err3 = new Exception("err3")

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val errorEffects = mutable.Buffer[Effect[String]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val obs = Observer.fromTry[Int] {
      case Success(value) => effects += Effect("obs", value)
      case Failure(err)   => errorEffects += Effect("obs-err", err.getMessage)
    }

    val bus = new EventBus[Int]
    val stream = bus.events
      .distinctErrors((e1, e2) => e1.getMessage == e2.getMessage)
      .map(Calculation.log("stream", calculations))

    stream.addObserver(obs)(testOwner)

    // --

    bus.writer.onError(err1)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err1")
    )

    errorEffects.clear()

    // --

    bus.writer.onError(err2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err2")
    )

    errorEffects.clear()

    // --

    bus.writer.onError(err2)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer()

    // --

    bus.writer.onError(err3)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err3")
    )

    errorEffects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("stream", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )
    errorEffects shouldBe mutable.Buffer()

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onError(err3)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()
    errorEffects shouldBe mutable.Buffer(
      Effect("obs-err", "err3")
    )

    errorEffects.clear()
  }
}
