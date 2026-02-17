package com.raquo.airstream.syntax

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observable, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.annotation.nowarn
import scala.concurrent.Future

class SyntaxSpec extends UnitSpec {

  case class Foo(id: Int, label: String)

  it("CombinableStream & TupleStream") {

    val bus = new EventBus[Int]
    val bus1 = new EventBus[Int]
    val bus2 = new EventBus[Boolean]
    val bus3 = new EventBus[String]

    case class Foo(a: Int, b: Int, c: Boolean, d: String)

    locally {
      val tuple4stream = bus.events.combineWith(bus1.events, bus2.events, bus3.events)
      tuple4stream: EventStream[(Int, Int, Boolean, String)]
    }

    locally {
      val tuple4stream = bus.events.combineWithFn(bus1.events, bus2.events, bus3.events)(Foo.apply)
      tuple4stream: EventStream[Foo]
    }

    locally {
      val fooStream = bus.events.withCurrentValueOf(
        bus1.events.startWith(0),
        bus2.events.startWith(false),
        bus3.events.startWith("")
      ).mapN(Foo.apply)
      fooStream: EventStream[Foo]
    }

    // --

    locally {
      val tuple4stream = bus.events.combineWith(bus1.events).combineWith(bus2.events, bus3.events)
      tuple4stream: EventStream[(Int, Int, Boolean, String)]
    }

    locally {
      val fooStream = bus.events.withCurrentValueOf(bus1.events.startWith(0)).withCurrentValueOf(
        bus2.events.startWith(false),
        bus3.events.startWith("")
      ).mapN(Foo.apply)
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
      val combinedStream = EventStream.combineWithFn(bus.events, bus1.events, bus2.events, bus3.events)(Foo.apply)
      combinedStream: EventStream[Foo]
    }
  }

  it("Replacement for ye olde SwitchFutureStrategy") {

    val bus = new EventBus[Int]

    locally {
      val flatStream = bus.events.flatMapSwitch(a => EventStream.fromFuture(Future.successful(a)))
      flatStream: EventStream[Int]
    }

    locally {
      val flatSignal = bus.events.startWith(0).flatMapSwitch(a => Signal.fromFuture(Future.successful(a), initial = 0))
      flatSignal: Signal[Int]
    }
  }

  it("toSignalIfStream / toStreamIfSignal") {

    val bus = new EventBus[Int]

    val obs = (bus.events: Observable[Int])

    val signal: Signal[Int] = bus.events.startWith(0)

    bus.events.toSignal(initial = 0)
    bus.events.toSignal(0)

    bus.events.toSignalIfStream(_.startWith(0))
    obs.toStreamIfSignal(_.updates)

    // I wish these wouldn't compile, but can't get =:= evidence to help me here.
    signal.toSignalIfStream(_.startWith(0))
    signal.toStreamIfSignal(_.updates)
    bus.events.toSignalIfStream(_.startWith(0))
    bus.events.toStreamIfSignal(_.updates)

    // -- Ensure weirdest type inference.

    val weirdBus = new EventBus[EventStream[Int] => Signal[Int]]
    val composer = (s: EventStream[Int]) => s.startWith(0)

    weirdBus.events.toSignalIfStream(_.startWith(composer))
    weirdBus.events.toSignal(composer)
  }

  it("Legacy methods from the video") {
    val v = Var(1)
    val vs = Var(Seq(Foo(1, "a"), Foo(2, "b"), Foo(3, "c")))
    ((v.signal.changes: @nowarn("msg=deprecated")): EventStream[Int])
    ((vs.signal.split(_.id) { (id, init, sig) =>
      (id, init, sig)
    }: @nowarn("msg=deprecated")): Signal[Seq[(Int, Foo, Signal[Foo])]])
    ((vs.signal.changes.split(_.id) { (id, init, sig) =>
      (id, init, sig)
    }: @nowarn("msg=deprecated")): Signal[Seq[(Int, Foo, Signal[Foo])]])
  }
}
