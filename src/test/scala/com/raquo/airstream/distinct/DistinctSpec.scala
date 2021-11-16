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
      case Failure(err) => errorEffects += Effect("obs-err", err.getMessage)
    }

    val bus = new EventBus[Int]
    val stream = bus.events
      .distinct
      .map(Calculation.log("stream", calculations))

    val sub1 = stream.addObserver(obs)(testOwner)

    // --

    bus.writer.onNext(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onNext(1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    val sub2 = stream.addObserver(obs)(testOwner)

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    bus.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 3)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    errorEffects shouldEqual mutable.Buffer() // nothing failed yet

    bus.writer.onError(err1)

    calculations shouldEqual mutable.Buffer()

    effects shouldEqual mutable.Buffer()

    errorEffects shouldEqual mutable.Buffer(
      Effect("obs-err", "err1")
    )

    errorEffects.clear()

    // -- errors are not distincted by the `distinct` method

    bus.writer.onError(err1)

    errorEffects shouldEqual mutable.Buffer(
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
      case Failure(err) => errorEffects += Effect("obs-err", err.getMessage)
    }

    val $var = Var(0)
    val signal = $var.signal
      .distinct
      .map(Calculation.log("signal", calculations))

    val sub1 = signal.addObserver(obs)(testOwner)

    // --

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 0)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 0)
    )

    calculations.clear()
    effects.clear()

    // --

    $var.writer.onNext(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 1)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    $var.writer.onNext(1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()

    // --

    $var.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    signal.addObserver(obs)(testOwner)

    $var.writer.onNext(2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer(
      Effect("obs", 2)
    )
    effects.clear()

    // --

    $var.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("signal", 3)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    errorEffects shouldEqual mutable.Buffer() // nothing failed yet

    $var.writer.onError(err1)

    calculations shouldEqual mutable.Buffer()

    effects shouldEqual mutable.Buffer()

    errorEffects shouldEqual mutable.Buffer(
      Effect("obs-err", "err1")
    )

    errorEffects.clear()

    // -- errors are not distincted by the `distinct` method

    $var.writer.onError(err1)

    errorEffects shouldEqual mutable.Buffer(
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
      case Failure(err) => errorEffects += Effect("obs-err", err.getMessage)
    }

    val bus = new EventBus[Int]
    val stream = bus.events
      .distinctErrors((e1, e2) => e1.getMessage == e2.getMessage)
      .map(Calculation.log("stream", calculations))

    stream.addObserver(obs)(testOwner)

    // --

    bus.writer.onError(err1)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("obs-err", "err1")
    )

    errorEffects.clear()

    // --

    bus.writer.onError(err2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("obs-err", "err2")
    )

    errorEffects.clear()

    // --

    bus.writer.onError(err2)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer()

    // --

    bus.writer.onError(err3)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("obs-err", "err3")
    )

    errorEffects.clear()

    // --

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("obs", 2)
    )
    errorEffects shouldEqual mutable.Buffer()

    calculations.clear()
    effects.clear()

    // --

    bus.writer.onError(err3)

    calculations shouldEqual mutable.Buffer()
    effects shouldEqual mutable.Buffer()
    errorEffects shouldEqual mutable.Buffer(
      Effect("obs-err", "err3")
    )

    errorEffects.clear()
  }
}
