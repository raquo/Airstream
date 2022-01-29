package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class DelayEventStreamSpec extends AsyncUnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  val effects = mutable.Buffer[Effect[Int]]()

  val obs1 = Observer[Int](effects += Effect("obs1", _))

  before {
    owner.killSubscriptions()
    effects.clear()
  }


  it("events are delayed, and purged on stop") {
    val bus = new EventBus[Int]
    val stream = bus.events.delay(30)

    val sub = stream.addObserver(obs1)

    delay {
      effects shouldBe mutable.Buffer()

      // --

      bus.writer.onNext(1)

      effects shouldBe mutable.Buffer()

    }.flatMap[Unit] { _ =>
      delay(30) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()

        bus.writer.onNext(2)
        bus.writer.onNext(3)

        assert(effects.isEmpty)
        ()
      }
    }.flatMap[Unit] { _ =>
      delay(30) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2), Effect("obs1", 3))
        effects.clear()

        bus.writer.onNext(4)
        bus.writer.onNext(5)

        sub.kill() // this kills pending events even if we immediately restart

        assert(effects.isEmpty)

        stream.addObserver(obs1)

        bus.writer.onNext(6)
      }
    }.flatMap { _ =>
      delay(40) { // a bit extra margin for the last check just to be sure that we caught any events
        effects shouldBe mutable.Buffer(Effect("obs1", 6))
        effects.clear()
        assert(true)
      }
    }
  }

}
