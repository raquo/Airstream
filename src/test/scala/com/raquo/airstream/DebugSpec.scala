package com.raquo.airstream

import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, EventStream, Observable, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.{Val, Var}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class DebugSpec extends UnitSpec with BeforeAndAfter {

  private val calculations = mutable.Buffer[Calculation[Try[Int]]]()

  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  private val errorCallback = (err: Throwable) => {
    errorEffects += Effect("unhandled", err)
    ()
  }

  implicit val owner: TestableOwner = new TestableOwner

  before {
    owner.killSubscriptions()
    calculations.clear()
    errorEffects.clear()
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    assert(errorEffects.isEmpty)
  }

  it("stream spy debuggers") {

    val bus = new EventBus[Int]

    // #Note: we write out `ev` excessively to make sure that type inference works

    val events = bus.events
      .debugSpy(ev => Calculation.log("bus.events", calculations)(ev))

    val events1 = events
      .debugSpy(ev => Calculation.log("events-1", calculations)(ev))
      .debugSpyStarts(_ => Calculation.log("events-1-start", calculations)(Success(-1)))
      .debugSpyStops(() => Calculation.log("events-1-stop", calculations)(Success(-1)))

    val events2 = events
      .debugSpyEvents(ev => Calculation.log("events-2", calculations)(Success(ev)))
      .debugSpyErrors(err => Calculation.log("events-2", calculations)(Failure(err)))
      .debugSpyLifecycle(
        startFn = _ => Calculation.log("events-2-start", calculations)(Success(-1)),
        stopFn = () => Calculation.log("events-2-stop", calculations)(Success(-1))
      )

    val obs1 = Observer.empty[Int].debugSpy(ev => Calculation.log("obs-1", calculations)(ev))

    val obs21 = Observer.empty[Int].debugSpy(ev => Calculation.log("obs-21", calculations)(ev))

    val obs22 = Observer.empty[Int].debugSpy(ev => Calculation.log("obs-22", calculations)(ev))

    val err1 = new Exception("err1")

    val err2 = new Exception("err2")

    assert(calculations.isEmpty)

    // --

    bus.emit(1)

    assert(calculations.toList == Nil)

    // --

    val sub1 = events1.addObserver(obs1)

    assert(calculations.toList == List(
      Calculation("events-1-start", Success(-1))
    ))

    calculations.clear()

    // --

    bus.emit(2)

    assert(calculations.toList == List(
      Calculation("bus.events", Success(2)),
      Calculation("events-1", Success(2)),
      Calculation("obs-1", Success(2))
    ))

    assert(errorEffects.toList == Nil)

    calculations.clear()

    // --

    bus.emitTry(Failure(err1))

    assert(calculations.toList == List(
      Calculation("bus.events", Failure(err1)),
      Calculation("events-1", Failure(err1)),
      Calculation("obs-1", Failure(err1))
    ))

    // #Note we test unhandled error propagation deliberately here.
    assert(errorEffects.toList == List(
      Effect("unhandled", err1)
    ))

    errorEffects.clear()

    calculations.clear()

    // --

    bus.emit(3)

    assert(calculations.toList == List(
      Calculation("bus.events", Success(3)),
      Calculation("events-1", Success(3)),
      Calculation("obs-1", Success(3))
    ))

    calculations.clear()

    // --

    sub1.kill()

    assert(calculations.toList == List(
      Calculation("events-1-stop", Success(-1))
    ))

    calculations.clear()

    // --

    events1.addObserver(obs1)

    events2.addObserver(obs21)

    events2.addObserver(obs22)

    assert(calculations.toList == List(
      Calculation("events-1-start", Success(-1)),
      Calculation("events-2-start", Success(-1))
    ))

    calculations.clear()

    // --

    bus.emit(4)

    assert(calculations.toList == List(
      Calculation("bus.events", Success(4)),
      Calculation("events-1", Success(4)),
      Calculation("obs-1", Success(4)),
      Calculation("events-2", Success(4)),
      Calculation("obs-21", Success(4)),
      Calculation("obs-22", Success(4))
    ))

    calculations.clear()

    assert(errorEffects.toList == Nil)

    // --

    bus.emitTry(Failure(err2))

    assert(calculations.toList == List(
      Calculation("bus.events", Failure(err2)),
      Calculation("events-1", Failure(err2)),
      Calculation("obs-1", Failure(err2)),
      Calculation("events-2", Failure(err2)),
      Calculation("obs-21", Failure(err2)),
      Calculation("obs-22", Failure(err2))
    ))

    // Three observers that don't handle errors => three unhandled errors
    assert(errorEffects.toList == List(
      Effect("unhandled", err2),
      Effect("unhandled", err2),
      Effect("unhandled", err2)
    ))

    calculations.clear()
    errorEffects.clear()
  }

  it("signal spy debuggers") {

    val v = Var(0)

    // #Note: we write out `ev` excessively to make sure that type inference works

    val signal = v.signal
      .debugSpyEvalFromParent(ev => Calculation.log("var.signal-eval-from-parent", calculations)(ev))
      .debugSpy(ev => Calculation.log("var.signal", calculations)(ev))

    val signal1 = signal
      .debugSpyEvalFromParent(ev => Calculation.log("signal-1-eval-from-parent", calculations)(ev))
      .debugSpyStarts(_ => Calculation.log("signal-1-start", calculations)(Success(-1)))
      .debugSpy(ev => Calculation.log("signal-1", calculations)(ev))
      .debugSpyStops(() => Calculation.log("signal-1-stop", calculations)(Success(-1)))

    val signal2 = signal
      .debugSpyEvalFromParent(ev => Calculation.log("signal-2-eval-from-parent", calculations)(ev))
      .debugSpyEvents(ev => Calculation.log("signal-2", calculations)(Success(ev)))
      .debugSpyErrors(err => Calculation.log("signal-2", calculations)(Failure(err)))
      .debugSpyLifecycle(
        startFn = _ => Calculation.log("signal-2-start", calculations)(Success(-1)),
        stopFn = () => Calculation.log("signal-2-stop", calculations)(Success(-1))
      )

    val obs1 = Observer.fromTry[Int] { case ev => Calculation.log("obs-1", calculations)(ev) }

    val obs21 = Observer.fromTry[Int] { case ev => Calculation.log("obs-21", calculations)(ev) }

    val obs22 = Observer.fromTry[Int] { case ev => Calculation.log("obs-22", calculations)(ev) }

    val err1 = new Exception("err1")

    val err2 = new Exception("err2")

    // --

    v.set(1)

    assert(calculations.toList == Nil)

    // --

    val sub1 = signal1.addObserver(obs1)

    // Order of logs is affected by order of debug statements above. It's slightly different for signal1 and signal2
    assert(calculations.toList == List(
      Calculation("var.signal-eval-from-parent", Success(1)),
      Calculation("signal-1-eval-from-parent", Success(1)),
      Calculation("obs-1", Success(1)),
      Calculation("var.signal", Success(1)),
      Calculation("signal-1-start", Success(-1)),
      Calculation("signal-1", Success(1)),
    ))

    calculations.clear()

    // --

    v.set(2)

    assert(calculations.toList == List(
      Calculation("var.signal", Success(2)),
      Calculation("signal-1", Success(2)),
      Calculation("obs-1", Success(2))
    ))

    calculations.clear()

    // --

    v.setTry(Failure(err1))

    assert(calculations.toList == List(
      Calculation("var.signal", Failure(err1)),
      Calculation("signal-1", Failure(err1)),
      Calculation("obs-1", Failure(err1))
    ))

    calculations.clear()

    // --

    v.set(3)

    assert(calculations.toList == List(
      Calculation("var.signal", Success(3)),
      Calculation("signal-1", Success(3)),
      Calculation("obs-1", Success(3))
    ))

    calculations.clear()

    // --

    sub1.kill()

    assert(calculations.toList == List(
      Calculation("signal-1-stop", Success(-1))
    ))

    calculations.clear()

    // --

    signal1.addObserver(obs1)

    signal2.addObserver(obs21)

    signal2.addObserver(obs22)

    assert(calculations.toList == List(
      Calculation("var.signal-eval-from-parent", Success(3)),
      Calculation("signal-1-eval-from-parent", Success(3)),
      Calculation("obs-1", Success(3)), // receive current value (initial value was already evaluated)
      Calculation("var.signal", Success(3)),
      Calculation("signal-1-start", Success(-1)),
      Calculation("signal-1", Success(3)),
      Calculation("signal-2-eval-from-parent", Success(3)),
      Calculation("obs-21", Success(3)),
      Calculation("signal-2", Success(3)),
      Calculation("signal-2-start", Success(-1)),
      Calculation("obs-22", Success(3)) // Adding obs21 triggered the full start first, THEN this obs22 was added.
    ))

    calculations.clear()

    // --

    v.set(4)

    assert(calculations.toList == List(
      Calculation("var.signal", Success(4)),
      Calculation("signal-1", Success(4)),
      Calculation("obs-1", Success(4)),
      Calculation("signal-2", Success(4)),
      Calculation("obs-21", Success(4)),
      Calculation("obs-22", Success(4))
    ))

    calculations.clear()

    // --

    v.setTry(Failure(err2))

    assert(calculations.toList == List(
      Calculation("var.signal", Failure(err2)),
      Calculation("signal-1", Failure(err2)),
      Calculation("obs-1", Failure(err2)),
      Calculation("signal-2", Failure(err2)),
      Calculation("obs-21", Failure(err2)),
      Calculation("obs-22", Failure(err2))
    ))

    calculations.clear()
  }

  it("observer spy debuggers") {

    val err1 = new Exception("err1")

    // #Note: we write out `ev` excessively to make sure that type inference works

    val obs = Observer
      .fromTry[Int] { case ev => Calculation.log("obs", calculations)(ev) }
      .debugSpy(ev => Calculation.log("obs-spy", calculations)(ev))
      .debugSpyEvents(ev => Calculation.log("obs-spy-events", calculations)(Success(ev)))
      .debugSpyErrors(err => Calculation.log("obs-spy-errors", calculations)(Failure(err)))

    assert(calculations.isEmpty)

    // --

    obs.onNext(1)

    // Order is the reverse of what it appears like at use site, which is as expected when contramapping observers.
    assert(calculations.toList == List(
      Calculation("obs-spy-events", Success(1)),
      Calculation("obs-spy", Success(1)),
      Calculation("obs", Success(1)),
    ))

    calculations.clear()

    assert(errorEffects.isEmpty)

    // --

    obs.onError(err1)

    assert(calculations.toList == List(
      Calculation("obs-spy-errors", Failure(err1)),
      Calculation("obs-spy", Failure(err1)),
      Calculation("obs", Failure(err1)),
    ))

    calculations.clear()

    // --

    obs.onTry(Success(2))

    assert(calculations.toList == List(
      Calculation("obs-spy-events", Success(2)),
      Calculation("obs-spy", Success(2)),
      Calculation("obs", Success(2)),
    ))

    calculations.clear()
  }

  it("observable displayName") {

    val bus = new EventBus[Int]

    // --

    val events = bus.events.setDisplayName("events")

    assert(events eq bus.events)
    assert(events.displayName == "events")
    assert(events.toString == "events")

    // --

    events.setDisplayName("bus.events")

    assert(events.displayName == "bus.events")
    assert(events.toString == "bus.events")

    // --

    assert(events.map(identity).displayName != "bus.events")
    assert(events.map(identity).toString != "bus.events")

    // --

    assert(events.debugLog().displayName == "bus.events|Debug")
    assert(events.debugLog().toString == "bus.events|Debug")
    assert(events.debugSpyErrors(_ => ()).debugLog().displayName == "bus.events|Debug")
    assert(events.debugSpyErrors(_ => ()).debugLog().toString == "bus.events|Debug")
    assert(events.debugSpyErrors(_ => ()).debugLog().debugLogStarts.displayName == "bus.events|Debug")
    assert(events.debugSpyErrors(_ => ()).debugLog().debugLogStarts.toString == "bus.events|Debug")

    // --

    assert(events.debugWithName("debugEvents") ne events)
    assert(events.displayName == "bus.events") // unchanged

    assert(events.debugWithName("debugEvents").displayName == "debugEvents")
    assert(events.debugWithName("debugEvents").toString == "debugEvents")
    assert(events.debugWithName("debugEvents").debugLog().displayName == "debugEvents")
    assert(events.debugWithName("debugEvents").debugLog().toString == "debugEvents")
    assert(events.debugWithName("debugEvents").debugSpy(_ => ()).debugLog().debugBreak().displayName == "debugEvents")
    assert(events.debugWithName("debugEvents").debugSpy(_ => ()).debugLog().debugBreak().toString == "debugEvents")

    assert(events.debugWithName("debugEvents").debugLog().map(identity).displayName != "debugEvents")
    assert(events.debugWithName("debugEvents").debugLog().map(identity).toString != "debugEvents")

    // --

    assert(events
      .debugWithName("debugEvents")
      .debugLog()
      .setDisplayName("debugLog")
      .displayName == "debugLog"
    )

    assert(events
      .debugWithName("debugEvents")
      .debugLog()
      .setDisplayName("debugLog")
      .toString == "debugLog"
    )

    assert(
      events
        .debugWithName("debugEvents")
        .debugLog()
        .setDisplayName("debugLog")
        .debugSpy(_ => ())
        .setDisplayName("debugSpy")
        .displayName == "debugSpy"
    )

    assert(
      events
        .debugWithName("debugEvents")
        .debugLog()
        .setDisplayName("debugLog")
        .debugSpy(_ => ())
        .setDisplayName("debugSpy")
        .toString == "debugSpy"
    )
  }

  it("observer displayName") {

    val obs = Observer.empty[Int]

    // --

    val obsWithNameSet = obs.setDisplayName("obs-0")

    assert(obsWithNameSet eq obs)
    assert(obs.displayName == "obs-0")
    assert(obs.toString == "obs-0")

    // --

    obs.setDisplayName("obs")

    assert(obs.displayName == "obs")
    assert(obs.toString == "obs")

    // --

    assert(obs.contramap[Int](identity).displayName != "obs")
    assert(obs.contramap[Int](identity).toString != "obs")

    // --

    assert(obs.debugLog().displayName == "obs|Debug")
    assert(obs.debugLog().toString == "obs|Debug")
    assert(obs.debugSpyErrors(_ => ()).debugLog().displayName == "obs|Debug")
    assert(obs.debugSpyErrors(_ => ()).debugLog().toString == "obs|Debug")
    assert(obs.debugSpyErrors(_ => ()).debugLog().debugLogEvents().displayName == "obs|Debug")
    assert(obs.debugSpyErrors(_ => ()).debugLog().debugLogEvents().toString == "obs|Debug")

    // --

    assert(obs.debugWithName("debugEvents") ne obs)
    assert(obs.displayName == "obs") // unchanged

    assert(obs.debugWithName("debugEvents").displayName == "debugEvents")
    assert(obs.debugWithName("debugEvents").toString == "debugEvents")
    assert(obs.debugWithName("debugEvents").debugLog().displayName == "debugEvents")
    assert(obs.debugWithName("debugEvents").debugLog().toString == "debugEvents")
    assert(obs.debugWithName("debugEvents").debugSpy(_ => ()).debugLog().debugBreak().displayName == "debugEvents")
    assert(obs.debugWithName("debugEvents").debugSpy(_ => ()).debugLog().debugBreak().toString == "debugEvents")

    assert(obs.debugWithName("debugEvents").debugLog().contramap[Int](identity).displayName != "debugEvents")
    assert(obs.debugWithName("debugEvents").debugLog().contramap[Int](identity).toString != "debugEvents")

    // --

    assert(obs
      //.debugWithName("debugEvents")
      .debugLog()
      .setDisplayName("debugLog")
      .displayName == "debugLog"
    )

    assert(obs
      //.debugWithName("debugEvents")
      .debugLog()
      .setDisplayName("debugLog")
      .toString == "debugLog"
    )

    assert(
      obs
        //.debugWithName("debugEvents")
        .debugLog()
        .setDisplayName("debugLog")
        .debugSpy(_ => ())
        .setDisplayName("debugSpy")
        .displayName == "debugSpy"
    )

    assert(
      obs
        //.debugWithName("debugEvents")
        .debugLog()
        .setDisplayName("debugLog")
        .debugSpy(_ => ())
        .setDisplayName("debugSpy")
        .toString == "debugSpy"
    )
  }

  it("observable debugger type inference") {

    EventStream.fromValue("a").debugSpy(_ => ()).debugLogStarts: Observable[String]

    EventStream.fromValue("a").debugSpy(_ => ()).debugLogStarts: EventStream[String]

    Val("a").debugSpy(_ => ()).debugLogStarts: Signal[String]
  }

  it("observable debugger error") {

    val bus = new EventBus[Int]

    val err0 = new Exception("err0")

    val err1 = new Exception("err1")

    val events = bus.events
      .debugSpy {
        case Success(ev) if ev < 10 =>
          Calculation.log("bus.events", calculations)(Success(ev))
        case _ =>
          throw err1
      }

    val obs = Observer.empty[Int].debugSpy(ev => Calculation.log("obs", calculations)(ev))

    events.addObserver(obs)

    assert(calculations.isEmpty)

    // --

    bus.emit(1)

    assert(calculations.toList == List(
      Calculation("bus.events", Success(1)),
      Calculation("obs", Success(1)),
    ))

    assert(errorEffects.isEmpty)

    calculations.clear()

    // -- Trigger error in debugger

    bus.emit(100)

    // bus.events calculation is not emitted because it only happens on the happy path
    assert(calculations.toList == List(
      Calculation("obs", Success(100)),
    ))

    assert(errorEffects.toList == List(
      Effect("unhandled", DebugError(err1, cause = None))
    ))

    calculations.clear()
    errorEffects.clear()

    // --

    bus.emit(2)

    assert(calculations.toList == List(
      Calculation("bus.events", Success(2)),
      Calculation("obs", Success(2)),
    ))

    assert(errorEffects.isEmpty)

    calculations.clear()

    // -- Trigger error in debugger with an error event cause

    bus.emitTry(Failure(err0))

    // bus.events calculation is not emitted because it only happens on the happy path
    assert(calculations.toList == List(
      Calculation("obs", Failure(err0)),
    ))

    assert(errorEffects.toList == List(
      Effect("unhandled", DebugError(err1, cause = Some(err0))),
      Effect("unhandled", err0) // We propagate the error event to the observer, but it doesn't handle that
    ))

    calculations.clear()
    errorEffects.clear()

    // --

    bus.emit(3)

    assert(calculations.toList == List(
      Calculation("bus.events", Success(3)),
      Calculation("obs", Success(3)),
    ))

    assert(errorEffects.isEmpty)

    calculations.clear()
  }

  it("observer debugger error") {

    val err0 = new Exception("err0")

    val err1 = new Exception("err1")

    val obs = Observer.fromTry[Int] { case ev => Calculation.log("obs", calculations)(ev) }
      .debugSpy {
        case Success(ev) if ev < 10 =>
          Calculation.log("obs.ok", calculations)(Success(ev))
        case _: Try[_] =>
          throw err1
      }

    // --

    obs.onNext(1)

    assert(calculations.toList == List(
      Calculation("obs.ok", Success(1)),
      Calculation("obs", Success(1)),
    ))

    calculations.clear()

    // -- Trigger error in debugger

    obs.onNext(100)

    // obs.ok calculation is not emitted because it only happens on the happy path
    assert(calculations.toList == List(
      Calculation("obs", Success(100)),
    ))

    assert(errorEffects.toList == List(
      Effect("unhandled", DebugError(err1, cause = None))
    ))

    calculations.clear()
    errorEffects.clear()

    // --

    obs.onNext(2)

    assert(calculations.toList == List(
      Calculation("obs.ok", Success(2)),
      Calculation("obs", Success(2)),
    ))

    assert(errorEffects.isEmpty)

    calculations.clear()

    // -- Trigger error in debugger with an error event cause

    obs.onError(err0)

    // bus.events calculation is not emitted because it only happens on the happy path
    assert(calculations.toList == List(
      Calculation("obs", Failure(err0)),
    ))

    assert(errorEffects.toList == List(
      Effect("unhandled", DebugError(err1, cause = Some(err0)))
    ))

    calculations.clear()
    errorEffects.clear()

    // --

    obs.onNext(3)

    assert(calculations.toList == List(
      Calculation("obs.ok", Success(3)),
      Calculation("obs", Success(3)),
    ))

    assert(errorEffects.isEmpty)

    calculations.clear()
  }
}
