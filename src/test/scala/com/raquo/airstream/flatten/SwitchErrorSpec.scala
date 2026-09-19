package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, ExpectedError, TestSource, TestableOwner}
import com.raquo.airstream.state.{Val, Var}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.Failure

/** Error-handling tests for the switch / flatMapSwitch / flattenSwitch family
  * (§5 of NOTES-flatMapSwitch-testing.md). Covers all four impls:
  *   - `SwitchStream` with an EventStream parent
  *   - `SwitchStream` with a Signal parent
  *   - `SwitchSignal` (Signal[Signal])
  *   - `SwitchSignalStream` (EventStream[Signal])
  *
  * Every flattened output is observed with `Observer.withRecover`, so a correctly
  * re-emitted error lands in `effects` as `Effect("error", <msg>)`. Any error that
  * instead escapes to the unhandled-error channel is caught by `unhandledErrors`
  * and fails the test in `after`.
  */
class SwitchErrorSpec extends UnitSpec with BeforeAndAfter {

  private val unhandledErrors = mutable.Buffer[Throwable]()

  private val errorCallback = (err: Throwable) => {
    unhandledErrors += err
    ()
  }

  before {
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    assert(unhandledErrors.isEmpty, s"Unexpected unhandled errors: ${unhandledErrors.toList}")
    unhandledErrors.clear()
  }

  private def recover(effects: mutable.Buffer[Effect[?]]): Observer[Int] =
    Observer.withRecover(
      v => effects += Effect("result", v),
      { case e => effects += Effect("error", e.getMessage) }
    )

  // -- SwitchStream, EventStream parent -------------------------------------------------

  it("SwitchStream (EventStream parent): a parent error is re-emitted, unsubscribes the current inner, and the switch recovers on the next value") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()
    val err = ExpectedError("parent-err")

    var updateA: Int => Unit = { _ => throw new Exception("innerA has not been started yet") }

    val innerA = TestSource.stream[Int](effects, "A", onStart = updateA = _)
    val innerB = new EventBus[Int]

    val metaBus = new EventBus[EventStream[Int]]

    metaBus.events.flattenSwitch.addObserver(recover(effects))

    metaBus.emit(innerA)

    assertEquals(effects.toList, List(Effect("A-start", "ix-1")))
    effects.clear()

    updateA(1)

    assertEquals(effects.toList, List(Effect("result", 1)))
    effects.clear()

    // -- parent error: the switch re-emits it and unsubscribes the current inner
    //    (`switchToNextError` removes the internal observer, so innerA stops).

    metaBus.writer.onError(err)

    assertEquals(
      effects.toList,
      List(
        Effect("A-stop", "ix-1"),
        Effect("error", "parent-err")
      )
    )
    effects.clear()

    // -- innerA is forgotten: its events no longer reach the switch

    updateA(2)

    assertEquals(effects.toList, Nil)

    // -- recover: switching to a new inner works normally

    metaBus.emit(innerB.events)

    innerB.writer.onNext(3)

    assertEquals(effects.toList, List(Effect("result", 3)))
    effects.clear()
  }

  it("SwitchStream (EventStream parent): an inner error is re-emitted; the inner stays subscribed, and switching still works") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()
    val err = ExpectedError("inner-err")

    val innerA = new EventBus[Int]
    val innerB = new EventBus[Int]

    val metaBus = new EventBus[EventStream[Int]]

    metaBus.events.flattenSwitch.addObserver(recover(effects))

    metaBus.emit(innerA.events)

    innerA.writer.onNext(1)

    assertEquals(effects.toList, List(Effect("result", 1)))
    effects.clear()

    // -- inner error: re-emitted (in a new transaction), but the switch does NOT
    //    unsubscribe from innerA.

    innerA.writer.onError(err)

    assertEquals(effects.toList, List(Effect("error", "inner-err")))
    effects.clear()

    // -- innerA is still mirrored after its error

    innerA.writer.onNext(2)

    assertEquals(effects.toList, List(Effect("result", 2)))
    effects.clear()

    // -- switching away still works, and innerA is then forgotten

    metaBus.emit(innerB.events)

    innerB.writer.onNext(3)

    assertEquals(effects.toList, List(Effect("result", 3)))
    effects.clear()

    innerA.writer.onNext(4)

    assertEquals(effects.toList, Nil)
  }

  // -- SwitchStream, Signal parent ------------------------------------------------------

  it("SwitchStream (Signal parent): a parent signal in a failed state at start emits the error on start, then recovers when the parent updates") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()
    val err = ExpectedError("parent-start-err")

    val parentVar = Var.fromTry[Int](Failure(err))

    parentVar.signal
      .flatMapSwitch(n => EventStream.fromValue(n * 10))
      .addObserver(recover(effects))

    // failed parent at start -> error emitted on start (in a new transaction,
    // via SwitchStream.onStart's Failure branch -> switchToNextError(transaction = None))
    assertEquals(effects.toList, List(Effect("error", "parent-start-err")))
    effects.clear()

    // recover: a fresh parent value re-derives and starts a normal inner
    parentVar.set(5)

    assertEquals(effects.toList, List(Effect("result", 50)))
    effects.clear()
  }

  // -- SwitchSignal (Signal[Signal]) ----------------------------------------------------

  it("SwitchSignal: a parent signal in a failed state at start emits the error, then recovers") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()
    val err = ExpectedError("meta-start-err")

    val metaVar = Var.fromTry[Signal[Int]](Failure(err))

    metaVar.signal.flattenSwitch.addObserver(recover(effects))

    // SwitchSignal's initial value is derived from the failed parent -> error on start
    assertEquals(effects.toList, List(Effect("error", "meta-start-err")))
    effects.clear()

    metaVar.set(Val(7))

    assertEquals(effects.toList, List(Effect("result", 7)))
    effects.clear()
  }

  it("SwitchSignal: an inner signal error is re-emitted, then recovers when the inner updates") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()
    val err = ExpectedError("inner-err")

    val innerVar = Var(0)
    val metaVar = Var[Signal[Int]](innerVar.signal)

    metaVar.signal.flattenSwitch.addObserver(recover(effects))

    assertEquals(effects.toList, List(Effect("result", 0)))
    effects.clear()

    innerVar.setError(err)

    assertEquals(effects.toList, List(Effect("error", "inner-err")))
    effects.clear()

    innerVar.set(5)

    assertEquals(effects.toList, List(Effect("result", 5)))
    effects.clear()
  }

  // -- SwitchSignalStream (EventStream[Signal]) -----------------------------------------

  it("SwitchSignalStream: an inner error is re-emitted (inner stays subscribed); a parent error is re-emitted and unsubscribes the current inner; recovers") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()
    val innerErr = ExpectedError("inner-err")
    val parentErr = ExpectedError("parent-err")

    val innerVar = Var(0)
    val innerVar2 = Var(100)

    val metaBus = new EventBus[Signal[Int]]

    metaBus.events.flattenSwitch.addObserver(recover(effects))

    metaBus.emit(innerVar.signal)

    assertEquals(effects.toList, List(Effect("result", 0)))
    effects.clear()

    // -- inner error: re-emitted, but the switch stays subscribed to innerVar

    innerVar.setError(innerErr)

    assertEquals(effects.toList, List(Effect("error", "inner-err")))
    effects.clear()

    innerVar.set(1)

    assertEquals(effects.toList, List(Effect("result", 1)))
    effects.clear()

    // -- parent error: re-emitted, and the current inner is unsubscribed

    metaBus.writer.onError(parentErr)

    assertEquals(effects.toList, List(Effect("error", "parent-err")))
    effects.clear()

    // -- innerVar is forgotten: its updates no longer reach the switch

    innerVar.set(2)

    assertEquals(effects.toList, Nil)

    // -- recover: switching to a new inner signal works normally

    metaBus.emit(innerVar2.signal)

    assertEquals(effects.toList, List(Effect("result", 100)))
    effects.clear()
  }

  // -- project (the flatMapSwitch mapping function) -------------------------------------

  it("flatMapSwitch: a throwing project function emits the error (guarded), and the switch recovers on the next value") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()
    val err = ExpectedError("project-err")

    val metaBus = new EventBus[Int]

    metaBus.events
      .flatMapSwitch(n => if (n == 0) throw err else EventStream.fromValue(n * 10))
      .addObserver(recover(effects))

    // project throws -> the error is emitted (map guards the projection), not thrown
    metaBus.emit(0)

    assertEquals(effects.toList, List(Effect("error", "project-err")))
    effects.clear()

    // recover: the next value projects normally
    metaBus.emit(5)

    assertEquals(effects.toList, List(Effect("result", 50)))
    effects.clear()
  }
}
