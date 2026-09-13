package com.raquo.airstream.extensions

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observable, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class OptionTupleObservablesSpec extends UnitSpec {

  it("mapSomes: stream -> stream, signal -> signal, observable -> observable") {

    implicit val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[(Option[Int], Option[Int])]
    val _var = Var[(Option[Int], Option[Int])]((Some(1), Some(2)))
    val observable: Observable[(Option[Int], Option[Int])] = _var.signal

    // Type checks (compile time): mapSomes preserves the observable kind

    val streamResult: EventStream[Option[Int]] = bus.events.mapSomes((a, b) => a + b)
    val signalResult: Signal[Option[Int]] = _var.signal.mapSomes((a, b) => a + b)
    val observableResult: Observable[Option[Int]] = observable.mapSomes((a, b) => a + b)

    // --

    val effects = mutable.Buffer[Effect[?]]()
    bus
      .events
      .mapSomes((a, b) => a + b)
      .foreach(v => effects += Effect("stream", v))

    effects shouldBe mutable.Buffer()

    bus.emit((Some(1), Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("stream", Some(3))
    )
    effects.clear()

    // --

    bus.emit((None, Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("stream", None)
    )
    effects.clear()

    // --

    bus.emit((Some(3), None))

    bus.emit((None, None))

    effects shouldBe mutable.Buffer() // consecutive None events are collapsed

    // --

    bus.emit((Some(10), Some(20)))

    effects shouldBe mutable.Buffer(
      Effect("stream", Some(30))
    )
    effects.clear()

    // --

    _var.signal
      .mapSomes((a, b) => a * 100 + b)
      .foreach(v => effects += Effect("signal", v))

    effects shouldBe mutable.Buffer(
      Effect("signal", Some(102))
    )
    effects.clear()

    _var.set((None, Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("signal", None)
    )
    effects.clear()

    _var.set((Some(3), None))

    effects shouldBe mutable.Buffer() // consecutive None events are collapsed
  }

  it("tupledSomes: stream -> stream, signal -> signal, observable -> observable") {

    implicit val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[(Option[Int], Option[Int])]
    val _var = Var[(Option[Int], Option[Int])]((Some(1), Some(2)))
    val observable: Observable[(Option[Int], Option[Int])] = _var.signal

    // Type checks (compile time): tupledSomes preserves the observable kind

    val streamResult: EventStream[Option[(Int, Int)]] = bus.events.tupledSomes
    val signalResult: Signal[Option[(Int, Int)]] = _var.signal.tupledSomes
    val observableResult: Observable[Option[(Int, Int)]] = observable.tupledSomes

    // --

    val effects = mutable.Buffer[Effect[?]]()
    bus
      .events
      .tupledSomes
      .foreach(v => effects += Effect("stream", v))

    effects shouldBe mutable.Buffer()

    bus.emit((Some(1), Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("stream", Some((1, 2)))
    )
    effects.clear()

    // --

    bus.emit((None, Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("stream", None)
    )
    effects.clear()

    // --

    bus.emit((Some(3), None))

    effects shouldBe mutable.Buffer() // consecutive None events are collapsed

    // --

    bus.emit((Some(10), Some(20)))

    effects shouldBe mutable.Buffer(
      Effect("stream", Some((10, 20)))
    )
    effects.clear()

    // --

    _var.signal
      .tupledSomes
      .foreach(v => effects += Effect("signal", v))

    effects shouldBe mutable.Buffer(
      Effect("signal", Some((1, 2)))
    )
    effects.clear()

    _var.set((None, Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("signal", None)
    )
    effects.clear()
  }

  it("splitOptions: stream and signal, yields Signal[Option[Out]]") {

    implicit val owner: TestableOwner = new TestableOwner

    val bus = new EventBus[(Option[Int], Option[Int])]
    val _var = Var[(Option[Int], Option[Int])]((Some(1), Some(2)))

    // Type checks (compile time): splitOptions always yields a Signal
    val streamSplit: Signal[Option[Int]] = bus.events.splitOptions((a, b) => a.now() + b.now())
    val signalSplit: Signal[Option[Int]] = _var.signal.splitOptions((a, b) => a.now() + b.now())

    // --

    val effects = mutable.Buffer[Effect[?]]()
    _var.signal
      .splitOptions((a, b) => a.now() + b.now())
      .foreach(v => effects += Effect("signal", v))

    effects shouldBe mutable.Buffer(
      Effect("signal", Some(3))
    )
    effects.clear()

    // --

    _var.set((None, Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("signal", None)
    )
    effects.clear()

    // --

    _var.set((Some(10), Some(20)))

    effects shouldBe mutable.Buffer(
      Effect("signal", Some(30))
    )
    effects.clear()

    // --

    bus.events
      .splitOptions((a, b) => a.now() + b.now())
      .foreach(v => effects += Effect("stream", v))

    effects shouldBe mutable.Buffer(
      Effect("stream", None) // treat stream as None before its first event
    )
    effects.clear()

    bus.emit((Some(1), Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("stream", Some(3))
    )
    effects.clear()

    // --

    bus.emit((None, Some(2)))

    effects shouldBe mutable.Buffer(
      Effect("stream", None)
    )
    effects.clear()

    // --

    bus.emit((Some(10), Some(20)))

    effects shouldBe mutable.Buffer(
      Effect("stream", Some(30))
    )
    effects.clear()
  }
}
