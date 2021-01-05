package com.raquo.airstream.syntax

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream

import scala.concurrent.Future

class SyntaxSpec extends UnitSpec {

  it("CombinableEventStream & TupleEventStream") {

    val bus = new EventBus[Int]
    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Boolean]
    val bus3 = new EventBus[String]

    case class Foo(a: Int, b: Int, c: Boolean, d: String)

    locally {
      val tuple4stream = bus.events.combine(bus1.events, bus2.events, bus3.events)
      tuple4stream: EventStream[(Int, Int, Boolean, String)]
    }

    locally {
      val tuple4stream = bus.events.combineWith(bus1.events, bus2.events, bus3.events)(Foo)
      tuple4stream: EventStream[Foo]
    }

    locally {
      val fooStream = bus.events.withCurrentValueOf(
        bus1.events.startWith(0),
        bus2.events.startWith(false),
        bus3.events.startWith("")
      ).mapN(Foo)
      fooStream: EventStream[Foo]
    }
  }

  it("{EventStream$,Signal$}.{combine,combineWith)") {

    val bus = new EventBus[Int]
    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Boolean]
    val bus3 = new EventBus[String]

    case class Foo(a: Int, b: Int, c: Boolean, d: String)

    locally {
      val combinedStream = EventStream.combine(bus1.events, bus2.events)
      combinedStream: EventStream[(Int, Boolean)]
    }

    locally {
      val combinedStream = EventStream.combineWith(bus.events, bus1.events, bus2.events, bus3.events)(Foo)
      combinedStream: EventStream[Foo]
    }
  }

  it("SwitchFutureStrategy") {

    val bus = new EventBus[Int]

    locally {
      val flatStream = bus.events.flatMap(a => Future.successful(a))
      flatStream: EventStream[Int]
    }

    locally {
      val flatSignal = bus.events.startWith(0).flatMap(a => Future.successful(a))
      flatSignal: EventStream[Int]
    }
  }
}
