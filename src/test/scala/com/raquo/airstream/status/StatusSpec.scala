package com.raquo.airstream.status

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class StatusSpec extends AsyncUnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  val effects = mutable.Buffer[Effect[Status[Int, String]]]()

  val obs1 = Observer[Status[Int, String]](effects += Effect("obs1", _))

  before {
    owner.killSubscriptions()
    effects.clear()
  }

  // #TODO[Test] Test with something like debounce to ensure that we are NOT
  //  creating a new output stream for every input stream, i.e. that we don't
  //  use flatMapSwitch inside the implementation.

  it("delayWithStatus, mapOutput") {
    val bus = new EventBus[Int]

    val stream = bus.events.delayWithStatus(30).mapOutput(_.toString)

    val sub = stream.addObserver(obs1)

    assert(effects.isEmpty)

    // --

    bus.emit(1)

    assertEquals(
      effects.toList,
      List(
        Effect("obs1", Pending(1))
      )
    )
    effects.clear()

    for {
      _ <- delay {
        assert(effects.isEmpty)
      }

      _ <- delay(30) {
        assertEquals(
          effects.toList,
          List(
            Effect("obs1", Resolved(1, "1", 1))
          )
        )
        effects.clear()

        // --

        bus.emit(2)

        assertEquals(
          effects.toList,
          List(
            Effect("obs1", Pending(2))
          )
        )
        effects.clear()
      }

      _ <- delay(30) {
        assertEquals(
          effects.toList,
          List(
            Effect("obs1", Resolved(2, "2", 1))
          )
        )
        effects.clear()
      }
    } yield {
      assert(true)
    }
  }

}
