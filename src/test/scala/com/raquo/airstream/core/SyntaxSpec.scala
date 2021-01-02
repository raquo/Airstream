package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream

class SyntaxSpec extends UnitSpec {

  it("CombinableEventStream") {

    val bus = new EventBus[Int]
    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Boolean]
    val bus3 = new EventBus[String]

    case class Foo(a: Int, b: Int, c: Boolean, d: String)

    locally {
      val tuple4stream = bus.events.combine(bus1.events, bus2.events, bus3.events)
      val _: EventStream[(Int, Int, Boolean, String)] = tuple4stream
    }

    locally {
      val tuple4stream = bus.events.combineWith(bus1.events, bus2.events, bus3.events)(Foo)
      val _: EventStream[Foo] = tuple4stream
    }

    //locally {
    //  val tuple4stream = bus.events.withCurrentValueOf(
    //    bus1.events.startWith(0),
    //    bus2.events.startWith(false),
    //    bus3.events.startWith("")
    //  ).map4(Foo)
    //  val _: EventStream[(Int, Int, Boolean, String)] = tuple4stream
    //}
  }
}
