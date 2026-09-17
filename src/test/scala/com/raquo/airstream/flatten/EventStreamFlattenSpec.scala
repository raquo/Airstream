package com.raquo.airstream.flatten

import com.raquo.airstream.{AsyncUnitSpec, Matchers}
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import scala.annotation.nowarn
import scala.collection.mutable

class EventStreamFlattenSpec extends AsyncUnitSpec with Matchers {

  private val done = assert(true)

  it("sync map-flatten") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream.setDisplayName("SRC-FS")
        .map { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true).setDisplayName(s"INT-FS-$v")
        }.setDisplayName("META")
        .flattenSwitch.setDisplayName("FLAT")

    val effects = mutable.Buffer[Effect[?]]()
    val obs0 = Observer[Int](newValue => effects += Effect("obs0", newValue)).setDisplayName("obs0")
    val subscription0 = flatStream.addObserver(obs0)

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3))
  }

  it("sync map-flatten without fromSeq") {

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[Int]
    val stream = bus.events
    val flatStream =
      stream
        .map { v =>
          EventStream.fromValue(v * 3, emitOnce = true).setDisplayName(s"S-${v}")
        }
        .setDisplayName("MO")
        .flattenSwitch.setDisplayName("FS")

    val effects = mutable.Buffer[Effect[?]]()
    val obs = Observer[Int](v => effects += Effect("obs0", v)).setDisplayName("obs")
    val subscription0 = flatStream.addObserver(obs)

    bus.emit(0)
    bus.emit(1)
    bus.emit(2)

    subscription0.kill()

    effects.toList shouldBe List(
      Effect("obs0", 0 * 3),
      Effect("obs0", 1 * 3),
      Effect("obs0", 2 * 3),
    )
  }

  it("sync three-level map-flatten") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .map { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true).map { vv =>
            EventStream.fromSeq(Seq(vv * 7), emitOnce = true)
          }.flattenSwitch
        }
        .flattenSwitch

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3 * 7))
  }

  private def delayedStream(points: Seq[Int], interval: Int, valueF: Int => Int = identity): EventStream[Int] = {
    val bus = new EventBus[Int]()
    points.foreach { i =>
      delay(i * interval) {
        bus.writer.onNext(valueF(i))
      }
    }
    bus.events
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("from-future map-flatten") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 30)

    val flatStream =
      stream
        .map { v =>
          delayedStream(range2, interval = 6, _ * v)
        }
        .flattenSwitch

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(150) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j =>
          Effect("obs0", i * j)
        )
      )
    }
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("three-level from-future map-flatten") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .map { v =>
          delayedStream(range2, interval = 6, _ * v).map { vv =>
            EventStream.fromFuture(delay(1)(vv * 7))
          }.flattenSwitch
        }
        .flattenSwitch

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j => Effect("obs0", i * j * 7))
      )
    }
  }

  it("sync flatMapSwitch") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .flatMapSwitch { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true)
        }

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3))
  }

  it("sync three-level flatMapSwitch") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .flatMapSwitch { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true).flatMapSwitch { vv =>
            EventStream.fromSeq(Seq(vv * 7), emitOnce = true)
          }
        }

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3 * 7))
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("from-future flatMapSwitch") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .flatMapSwitch { v =>
          delayedStream(range2, interval = 6, _ * v)
        }

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j =>
          Effect("obs0", i * j)
        )
      )
    }
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("three-level from-future flatMapSwitch") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .flatMapSwitch { v =>
          delayedStream(range2, interval = 6, _ * v).flatMapSwitch { vv =>
            EventStream.fromFuture(delay(1)(vv * 7))
          }
        }

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j => Effect("obs0", i * j * 7))
      )
    }
  }

  it("legacy flatMap and flatten methods") {

    val bus = new EventBus[Int]

    assertTypeError("bus.events.flatMap(_ => EventStream.fromValue(1))")

    @nowarn("cat=deprecation")
    def flatMapCompileCheck() = {
      import com.raquo.airstream.flatten.FlattenStrategy.allowFlatMap
      bus.events.flatMap(_ => EventStream.fromValue(1))
    }

    // --

    assertTypeError("bus.events.map(_ => EventStream.fromValue(1)).flatten")

    @nowarn("cat=deprecation")
    def flattenCompileCheck() = {
      import com.raquo.airstream.flatten.FlattenStrategy.allowFlatten
      bus.events.map(_ => EventStream.fromValue(1)).flatten
    }

    done
  }
}
