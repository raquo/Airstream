package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestSource, TestableOwner}
import com.raquo.airstream.state.{Val, Var}

import scala.collection.mutable
import scala.util.{Success, Try}

class SwitchSignalSpec extends UnitSpec {

  it("mirrors last emitted signal, but only if subscribed") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    // Create 4 test vars and add logging to their streams
    val sourceVars = (1 to 4).map(_ => Var(-1))
    val sourceSignals = sourceVars.zipWithIndex.map {
      case (vr, index) => vr.signal.map(Calculation.log(s"source-$index", calculations))
    }

    val metaVar = Var(sourceSignals(0))

    val latestNumberS = metaVar.signal.flattenSwitch // SwitchSignalStrategy is the default (provided implicitly)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenSignal = latestNumberS.map(Calculation.log("flattened", calculations))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    val subFlatten = flattenSignal.addObserver(flattenObserver)

    calculations shouldBe mutable.Buffer(
      Calculation("source-0", -1),
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
      Calculation("source-0", 0),
      Calculation("flattened", 0)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 0)
    )

    calculations.clear()
    effects.clear()

    // --

    metaVar.writer.onNext(sourceSignals(1))

    calculations shouldBe mutable.Buffer(
      Calculation("source-1", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    sourceVars(1).writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("source-1", 1),
      Calculation("flattened", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    metaVar.writer.onNext(sourceSignals(2))

    val source2Observer = Observer[Int](effects += Effect("source-2-obs", _))

    val source2Sub = sourceSignals(2).addObserver(source2Observer)
    subFlatten.kill()

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", -1),
      Calculation("flattened", -1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1),
      Effect("source-2-obs", -1)
    )

    calculations.clear()
    effects.clear()

    // --

    // flattened signal does not redo any calculations because
    // neither the current signal nor the parent signal emitted
    // anything while the flattened signal was stopped.

    val subFlatten2 = flattenSignal.addObserver(flattenObserver)

    calculations.shouldBeEmpty
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", -1)
    )

    effects.clear()

    subFlatten2.kill()

    // --

    sourceVars(2).writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    // flattened signal pulls current value from current signal on restart
    // because current signal emitted while flatten signal was stopped

    flattenSignal.addObserver(flattenObserver) // re-activate flattened signal

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
      Calculation("source-2", 3),
      Calculation("flattened", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 3),
      Effect("flattened-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    source2Sub.kill()

    sourceVars(2).writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 4),
      Calculation("flattened", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 4)
    )

    calculations.clear()
    effects.clear()
  }

  it("Signal: emitting the same inner signal does not cause it to stop and re-start") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[String]]()

    // It's important that we reuse the exact same references to inner signals to check the logic
    // - fromSeq streams are used to ensure that onStart isn't called extraneously
    // - bus.events streams are used to ensure that onStop isn't called extraneously

    val outerBus = new EventBus[Int].setDisplayName("outerBus")

    val smallBus = new EventBus[String].setDisplayName("smallBus")

    val bigBus = new EventBus[String].setDisplayName("bigBus")

    val smallSignal = EventStream.merge(
      smallBus.events.setDisplayName("smallBus.events"),
      EventStream.fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true).setDisplayName("smallSeq")
    ).setDisplayName("smallMerged").startWith("small-0").setDisplayName("smallSignal")

    val bigSignal = EventStream.merge(
      bigBus.events.setDisplayName("bigBus.events"),
      EventStream.fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true).setDisplayName("bigSeq")
    ).setDisplayName("bigMerged").startWith("big-0").setDisplayName("bigSignal")

    val flatSignal = outerBus.events.setDisplayName("outerBus.events").startWith(0).setDisplayName("outerBus.signal").map {
      case i if i >= 10 => bigSignal
      case _ => smallSignal
    }.setDisplayName("outerBus.meta").flattenSwitch.setDisplayName("flatSignal").map(Calculation.log("flat", calculations))

    // --

    val emptyObs = Observer.empty.setDisplayName("emptyObs")

    flatSignal.addObserver(emptyObs)

    assert(calculations.toList == List(
      Calculation("flat", "small-0"),
      Calculation("flat", "small-1"),
      Calculation("flat", "small-2"),
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-0")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-0")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(1)

    assert(calculations.toList == Nil)

    // --

    smallBus.writer.onNext("small-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(2)

    assert(calculations.toList == Nil)

    // --

    smallBus.writer.onNext("small-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(10) // #Note switch to big

    assert(calculations.toList == List(
      Calculation("flat", "big-0"),
      Calculation("flat", "big-1"),
      Calculation("flat", "big-2")
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small bus - unrelated change")

    assert(calculations.isEmpty)

    // --

    bigBus.writer.onNext("big-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(11)

    assert(calculations.toList == Nil)

    // --

    bigBus.writer.onNext("big-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(5) // #Note switch back to small

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2") // Restore current value of small signal
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-3")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-3")
    ))

    calculations.clear()

    // --

    bigBus.writer.onNext("big bus - unrelated change")

    assert(calculations.isEmpty)
  }

  it("map parent inside flatMap") {

    // @see https://github.com/raquo/Airstream/issues/95

    val effects = mutable.Buffer[Effect[String]]()

    var smallI = -1
    var bigI = -1

    val owner = new TestableOwner

    val intVar = Var(2000)

    val intSignal = intVar.signal

    val brokenSignal =
      intSignal
        .flatMapSwitch { num =>
          if (num < 1000) {
            smallI += 1
            intSignal.map("small: " + _).setDisplayName(s"small-$smallI") // .debugLogLifecycle()
          } else {
            bigI += 1
            Val("big").setDisplayName(s"val-$bigI")
          }
        }

    brokenSignal.foreach(Effect.log("output", effects))(owner)

    def setVar(v: Int): Unit = {
      Effect.log("set", effects)(v.toString)
      intVar.set(v)
    }

    // --

    setVar(884)
    setVar(887)
    setVar(1018)
    setVar(1141)
    setVar(1142)

    effects shouldBe mutable.Buffer(
      Effect("output", "big"),
      Effect("set", "884"),
      Effect("output", "small: 884"),
      Effect("set", "887"),
      Effect("output", "small: 887"),
      Effect("set", "1018"),
      Effect("output", "big"),
      Effect("set", "1141"),
      Effect("output", "big"),
      Effect("set", "1142"),
      Effect("output", "big")
    )
  }

  it("Switching between two signals does not cause their common ancestor to briefly stop") {

    val owner = new TestableOwner

    val effects = mutable.Buffer[Effect[?]]()

    var updateSource: Try[Int] => Unit = _ => throw new Exception("source signal has not been started yet")

    val source = Signal.fromCustomSource[Int](
      initial = Success(1),
      start = (setCurrValue, getCurrValue, getStartIx, getIsStarted) => {
        updateSource = setCurrValue
        effects += Effect("source-start", "ix-" + getStartIx())
      },
      stop = startIx => {
        effects += Effect("source-stop", "ix-" + startIx)
      }
    )

    val sig1 = source.map(_ * 10)
    val sig2 = source.map(_ * 100)

    val switch = Var(1)

    switch
      .signal
      .flatMapSwitch { v =>
        effects += Effect("switch", v)
        if (v % 2 == 0) sig1 else sig2
      }
      .foreach(v => {
        effects += Effect("result", v)
      })(owner)

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

    // --

    switch.set(2)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 2),
        Effect("result", 20)
      )
    )
    effects.clear()

    // --

    switch.set(3)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 3),
        Effect("result", 200)
      )
    )
    effects.clear()

    // --

    updateSource(Success(4))

    assertEquals(
      effects.toList,
      List(
        Effect("result", 400)
      )
    )
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

    val metaVar = Var[Signal[Int]](innerA)

    metaVar.signal.flattenSwitch.foreach(v => effects += Effect("result", v))(owner)

    // On start, the flattened signal mirrors innerA (its current value is 0).
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
    //    innerA must NOT be stopped (make-before-break plus the independent
    //    observer keep it running), while the flattened signal now mirrors innerB.

    val extSubA = innerA.addObserver(Observer.empty)

    assertEquals(effects.toList, Nil) // innerA already running, no extra start

    metaVar.set(innerB)

    assertEquals(
      effects.toList,
      List(
        Effect("result", 100),
        Effect("B-start", "ix-1")
      )
    )
    effects.clear()

    // -- innerA's updates no longer reach the flattened signal (switch forgot it),
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

    metaVar.set(innerA)

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

  it("re-syncs current value on restart iff the inner signal changed while stopped (no spurious event)") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val innerVar = Var(0)
    val metaVar = Var[Signal[Int]](innerVar.signal)

    // `map(Calculation.log)` lets us distinguish a real re-sync (the SwitchSignal
    // fired a new value, so the downstream map recomputes) from the ordinary
    // "a signal hands its current value to a new observer" (no recompute).
    val flatSignal = metaVar.signal.flattenSwitch.map(Calculation.log("flat", calculations))

    val obs = Observer[Int](effects += Effect("obs", _))

    val sub1 = flatSignal.addObserver(obs)

    assertEquals(calculations.toList, List(Calculation("flat", 0)))
    assertEquals(effects.toList, List(Effect("obs", 0)))
    calculations.clear()
    effects.clear()

    innerVar.set(1)

    assertEquals(calculations.toList, List(Calculation("flat", 1)))
    assertEquals(effects.toList, List(Effect("obs", 1)))
    calculations.clear()
    effects.clear()

    // -- stop, restart WITHOUT changing the inner signal: no re-sync (update id
    //    unchanged), so no recompute. The new observer still gets the current value once.

    sub1.kill()

    val sub2 = flatSignal.addObserver(obs)

    assertEquals(calculations.toList, Nil)
    assertEquals(effects.toList, List(Effect("obs", 1)))
    effects.clear()

    // -- stop, change the inner signal while stopped (Var retains its value and bumps
    //    its update id), restart: SwitchSignal re-syncs the new value exactly once.

    sub2.kill()

    innerVar.set(2)

    val sub3 = flatSignal.addObserver(obs)

    assertEquals(calculations.toList, List(Calculation("flat", 2)))
    assertEquals(effects.toList, List(Effect("obs", 2)))
    calculations.clear()
    effects.clear()

    // -- and it keeps mirroring after restart

    innerVar.set(3)

    assertEquals(calculations.toList, List(Calculation("flat", 3)))
    assertEquals(effects.toList, List(Effect("obs", 3)))
    calculations.clear()
    effects.clear()

    sub3.kill()
  }
}
