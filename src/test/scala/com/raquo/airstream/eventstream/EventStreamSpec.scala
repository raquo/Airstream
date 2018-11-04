package com.raquo.airstream.eventstream

import com.raquo.airstream.AsyncSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import scala.collection.mutable

class EventStreamSpec extends AsyncSpec {

  it("filter works") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val range = 0 to 10
    val stream = EventStream.fromSeq(range)

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = stream.filter(f).foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.filter(f).map(i => Effect("obs0", i))
  }

  it("filterNot works") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val range = 0 to 10
    val stream = EventStream.fromSeq(range)

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = stream.filterNot(f).foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.filterNot(f).map(i => Effect("obs0", i))
  }

  // @TODO[Test] move the tests below somewhere else?

  it("sync map-flatten works") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range)
    val flatStream =
      stream
        .map { v =>
          EventStream.fromSeq(Seq(v * 3))
        }
        .flatten

    val effects = mutable.Buffer[Effect[_]]()
    val subscription0 = flatStream.foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.map(i => Effect("obs0", i*3))
  }

  it("sync three-level map-flatten works") {

    implicit val owner: Owner = new TestableOwner

    val range = 0 to 3
    val stream = EventStream.fromSeq(range)
    val flatStream =
      stream
        .map { v =>
          EventStream.fromSeq(Seq(v * 3)).map { vv =>
            EventStream.fromSeq(Seq(vv * 7))
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
    val stream = delayedStream(range1, interval = 30)

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

    delay(150) {
      subscription0.kill()
      effects.toList shouldBe range1.flatMap(i =>
        range2.map(j => Effect("obs0", i * j * 7))
      )
    }
  }

}
