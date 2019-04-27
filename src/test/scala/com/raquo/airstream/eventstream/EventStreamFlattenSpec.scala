package com.raquo.airstream.eventstream

import com.raquo.airstream.AsyncSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import scala.collection.mutable

class EventStreamFlattenSpec extends AsyncSpec {

  it("sync map-flatten works") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .map { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true)
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3))
  }

  it("sync three-level map-flatten works") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .map { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true).map { vv =>
            EventStream.fromSeq(Seq(vv * 7), emitOnce = true)
          }.flatten
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
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
  it("from-future map-flatten works") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 30)

    val flatStream =
      stream
        .map { v =>
          delayedStream(range2, interval = 6, _ * v)
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
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
  it("three-level from-future map-flatten works") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .map { v =>
          delayedStream(range2, interval = 6, _ * v).map { vv =>
            EventStream.fromFuture(delay(1)(vv * 7))
          }.flatten
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j => Effect("obs0", i * j * 7))
      )
    }
  }

  it("sync flatMap works") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .flatMap { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true)
        }

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3))
  }

  it("sync three-level flatMap works") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range, emitOnce = true)
    val flatStream =
      stream
        .flatMap { v =>
          EventStream.fromSeq(Seq(v * 3), emitOnce = true).flatMap { vv =>
            EventStream.fromSeq(Seq(vv * 7), emitOnce = true)
          }
        }

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i * 3 * 7))
  }

  /** Stability: make sure the outer delayedStream interval is large enough to ensure all events
    * emitted by the inner delayedStream are processed. Just because the interval is set to 6ms
    * does not mean that this is what it will be. It's merely the lower bound.
    */
  it("from-future flatMap works") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .flatMap { v =>
          delayedStream(range2, interval = 6, _ * v)
        }

    val effects = mutable.Buffer[Effect[_]]()
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
  it("three-level from-future flatMap works") {
    implicit val owner: Owner = new TestableOwner

    val range1 = 1 to 3
    val range2 = 1 to 2
    val stream = delayedStream(range1, interval = 40)

    val flatStream =
      stream
        .flatMap { v =>
          delayedStream(range2, interval = 6, _ * v).flatMap { vv =>
            EventStream.fromFuture(delay(1)(vv * 7))
          }
        }

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    delay(200) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j => Effect("obs0", i * j * 7))
      )
    }
  }

}
