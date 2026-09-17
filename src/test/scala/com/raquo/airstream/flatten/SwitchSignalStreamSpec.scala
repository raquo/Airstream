package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestSource, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable
import scala.util.{Success, Try}

class SwitchSignalStreamSpec extends UnitSpec {

  it("mirrors last emitted signal, but only if subscribed") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    // Create 4 test vars and add logging to their streams
    val sourceVars = (1 to 4).map(_ => Var(-1))
    val sourceSignals = sourceVars.zipWithIndex.map {
      case (vr, index) => vr.signal.map(Calculation.log(s"signal-$index", calculations))
    }

    val metaBus = new EventBus[Signal[Int]]

    val latestNumberS = metaBus.events.flattenSwitch // SwitchSignalStreamStrategy is the default (provided implicitly)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenStream = latestNumberS.map(Calculation.log("flattened", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val subFlatten = flattenStream.addObserver(flattenObserver)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    metaBus.emit(sourceSignals(0))

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(0).writer.onNext(0)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0", 0),
      Calculation("flattened", 0)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 0)
    )

    calculations.clear()
    effects.clear()

    // --

    metaBus.writer.onNext(sourceSignals(1))

    calculations shouldBe mutable.Buffer(
      Calculation("signal-1", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // -- re-emit the same signal

    metaBus.writer.onNext(sourceSignals(1))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceVars(1).writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-1", 1),
      Calculation("flattened", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    metaBus.writer.onNext(sourceSignals(2))

    val source2Observer = Observer[Int](effects += Effect("signal-2-obs", _))

    val source2Sub = sourceSignals(2).addObserver(source2Observer)
    subFlatten.kill()

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1),
      Effect("signal-2-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(2).writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-2-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    // This stream fires an event with the current signal's current value when restarting,
    // IF the signal has emitted while the stream was stopped. This is similar to the new
    // signal.changes restart logic.

    flattenStream.addObserver(flattenObserver) // re-activate flattened signal

    calculations shouldBe mutable.Buffer(
      Calculation("flattened", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 2)
    )
    calculations.clear()
    effects.clear()

    // --

    sourceVars(2).writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", 3),
      Calculation("flattened", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("signal-2-obs", 3),
      Effect("flattened-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    source2Sub.kill()

    sourceVars(2).writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-2", 4),
      Calculation("flattened", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 4)
    )

    calculations.clear()
    effects.clear()
  }

  it("start & restart event order") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val manualBus = new EventBus[Int]

    // Create 4 test vars and add logging to their streams
    val sourceStreams = (1 to 4).map(n => EventStream.merge(manualBus.events, EventStream.fromSeq(List(1, 2).map(n * 10 + _))))
    val sourceSignals = sourceStreams.zipWithIndex.map {
      case (stream, index) => stream.startWith(0).map(Calculation.log(s"signal-$index", calculations))
    }

    val metaBus = new EventBus[Signal[Int]]

    val latestNumberS = metaBus.events.flattenSwitch // SwitchSignalStreamStrategy is the default (provided implicitly)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenStream = latestNumberS.map(Calculation.log("flattened", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val sub1 = flattenStream.addObserver(flattenObserver)

    calculations.shouldBeEmpty
    effects.shouldBeEmpty

    // --

    val source0 = sourceSignals(0)

    metaBus.emit(source0)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0", 0),
      Calculation("flattened", 0),
      Calculation("signal-0", 11),
      Calculation("flattened", 11),
      Calculation("signal-0", 12),
      Calculation("flattened", 12)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 0),
      Effect("flattened-obs", 11),
      Effect("flattened-obs", 12)
    )
    calculations.clear()
    effects.clear()

    // -- EventStream.fromSeq re-emits values on restart

    sub1.kill()

    val sub2 = flattenStream.addObserver(flattenObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0", 11),
      Calculation("flattened", 11),
      Calculation("signal-0", 12),
      Calculation("flattened", 12)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 11),
      Effect("flattened-obs", 12)
    )
    calculations.clear()
    effects.clear()

    // --

    val tempSub = source0.addObserver(Observer.empty)(owner)

    sub2.kill()

    manualBus.emit(10)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-0", 10)
    )
    effects shouldBe mutable.Buffer()

    calculations.clear()

    // -- when restarting, the flattened stream should emit the current value (10)
    //    first, followed by 11 and 12.

    tempSub.kill()

    val sub3 = flattenStream.addObserver(flattenObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("flattened", 10),
      Calculation("signal-0", 11),
      Calculation("flattened", 11),
      Calculation("signal-0", 12),
      Calculation("flattened", 12)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 10),
      Effect("flattened-obs", 11),
      Effect("flattened-obs", 12)
    )
    calculations.clear()
    effects.clear()

    // --

    metaBus.emit(sourceSignals(1))

    calculations shouldBe mutable.Buffer(
      Calculation("signal-1", 0),
      Calculation("flattened", 0),
      Calculation("signal-1", 21),
      Calculation("flattened", 21),
      Calculation("signal-1", 22),
      Calculation("flattened", 22)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 0),
      Effect("flattened-obs", 21),
      Effect("flattened-obs", 22)
    )
    calculations.clear()
    effects.clear()

    // --

    manualBus.emit(20)

    calculations shouldBe mutable.Buffer(
      Calculation("signal-1", 20),
      Calculation("flattened", 20),
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 20)
    )
    calculations.clear()
    effects.clear()

  }

  it("switching away drops the previous inner signal (stops it unless it has another observer)") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()

    var updateA: Try[Int] => Unit = { _ => throw new Exception("innerA has not been started yet") }
    var updateB: Try[Int] => Unit = { _ => throw new Exception("innerB has not been started yet") }

    // Two independent inner signals (no shared ancestor) so that we can observe
    // start/stop of each one directly via the instrumented custom source.
    val innerA = TestSource.signal[Int](
      effects = effects, label = "A", initial = Success(0), onStart = { updateA = _ }
    )
    val innerB = TestSource.signal[Int](
      effects = effects, label = "B", initial = Success(100), onStart = { updateB = _ }
    )

    // EventStream parent, driven through the flatMapSwitch(project) entry point.
    val intBus = new EventBus[Int]

    intBus.events
      .flatMapSwitch(n => if (n < 10) innerA else innerB)
      .foreach(v => effects += Effect("result", v))(owner)

    assertEquals(effects.toList, Nil) // nothing is mirrored until the parent emits a signal

    // -- switch to innerA

    intBus.emit(0)

    // #Note the signal's current value is emitted before the inner is started.
    assertEquals(
      effects.toList,
      List(
        Effect("result", 0),
        Effect("A-start", "ix-1")
      )
    )
    effects.clear()

    updateA(Success(1))

    assertEquals(effects.toList, List(Effect("result", 1)))
    effects.clear()

    // -- give innerA an independent observer, then switch away to innerB.
    //    innerA must NOT be stopped, while the flattened stream now mirrors innerB.

    val extSubA = innerA.addObserver(Observer.empty)

    assertEquals(effects.toList, Nil) // innerA already running, no extra start

    intBus.emit(10)

    assertEquals(
      effects.toList,
      List(
        Effect("result", 100),
        Effect("B-start", "ix-1")
      )
    )
    effects.clear()

    // -- innerA's updates no longer reach the flattened stream (switch forgot it),
    //    but innerA keeps running (via extSubA), so it retains this new value (2)

    updateA(Success(2))

    assertEquals(effects.toList, Nil)

    // -- ... but innerB's updates do

    updateB(Success(3))

    assertEquals(effects.toList, List(Effect("result", 3)))
    effects.clear()

    // -- killing innerA's independent observer finally stops it

    extSubA.kill()

    assertEquals(effects.toList, List(Effect("A-stop", "ix-1")))
    effects.clear()

    // -- switching back to innerA re-subscribes it from scratch (the switch had
    //    forgotten it), so it starts again (ix-2) and re-syncs its retained value (2).
    //    Thanks to make-before-break, innerB is stopped last, after innerA is running.

    intBus.emit(5)

    // #Note make-before-break: innerA is re-synced and started before innerB is stopped
    assertEquals(
      effects.toList,
      List(
        Effect("result", 2),
        Effect("A-start", "ix-2"),
        Effect("B-stop", "ix-1")
      )
    )
    effects.clear()
  }

  it("Switching between two signals does not cause their common ancestor to briefly stop") {

    val owner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()

    var updateSource: Try[Int] => Unit = { _ => throw new Exception("source signal has not been started yet") }

    val source = TestSource.signal[Int](
      effects = effects, label = "source", initial = Success(1), onStart = { updateSource = _ }
    )

    val sig1 = source.map(_ * 10)
    val sig2 = source.map(_ * 100)

    // EventStream parent, driven through the flatMapSwitch(project) entry point.
    val switchBus = new EventBus[Int]

    switchBus
      .events
      .flatMapSwitch { v =>
        effects += Effect("switch", v)
        if (v % 2 == 0) sig1 else sig2
      }
      .foreach(v => {
        effects += Effect("result", v)
      })(owner)

    assertEquals(effects.toList, Nil) // nothing is mirrored until the parent emits a signal

    // --

    switchBus.emit(1)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 1),
        Effect("result", 100),
        Effect("source-start", "ix-1")
      )
    )
    effects.clear()

    // --

    updateSource(Success(2))

    assertEquals(
      effects.toList,
      List(
        Effect("result", 200)
      )
    )
    effects.clear()

    // -- switching between sig1 and sig2 (which share `source`) must NOT stop and
    //    restart `source` (no source-stop / source-start), thanks to make-before-break.

    switchBus.emit(2)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 2),
        Effect("result", 20)
      )
    )
    effects.clear()

    // --

    switchBus.emit(3)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 3),
        Effect("result", 200)
      )
    )
    effects.clear()

    // -- the shared `source` was never stopped, so it keeps its running subscription

    updateSource(Success(4))

    assertEquals(
      effects.toList,
      List(
        Effect("result", 400)
      )
    )
    effects.clear()
  }

  it("restart re-emits the current signal's value iff it changed while stopped (update-id logic)") {

    // Complements "start & restart event order" above, which exercises the
    // fromSeq-re-emits-on-restart path and a changed-while-stopped emit; this test
    // isolates the update-id guard, including the clean unchanged-while-stopped case.

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val innerVar = Var(0)

    val metaBus = new EventBus[Signal[Int]]

    val flatStream = metaBus.events.flattenSwitch

    val sub1 = flatStream.foreach(v => effects += Effect("result", v))

    // switching to a signal emits its current value
    metaBus.emit(innerVar.signal)

    assertEquals(effects.toList, List(Effect("result", 0)))
    effects.clear()

    innerVar.set(1)

    assertEquals(effects.toList, List(Effect("result", 1)))
    effects.clear()

    // -- stop, restart WITHOUT changing the inner signal: its update id is unchanged,
    //    so nothing is re-emitted on restart (a stream is not a signal; it has no
    //    current value to hand out).

    sub1.kill()

    val sub2 = flatStream.foreach(v => effects += Effect("result", v))

    assertEquals(effects.toList, Nil)

    // -- stop, change the inner signal while stopped (Var retains its value and bumps
    //    its update id), restart: the current value is re-emitted exactly once.

    sub2.kill()

    innerVar.set(2)

    val sub3 = flatStream.foreach(v => effects += Effect("result", v))

    assertEquals(effects.toList, List(Effect("result", 2)))
    effects.clear()

    // -- and it keeps mirroring after restart

    innerVar.set(3)

    assertEquals(effects.toList, List(Effect("result", 3)))
    effects.clear()

    sub3.kill()
  }
}
