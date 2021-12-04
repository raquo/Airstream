package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class PullResetSignalSpec extends UnitSpec {

  it("ChangesStream & startWith") {

    test(CACHE_INITIAL_VALUE = false/*, EMIT_CHANGE_ON_RESTART = true*/)
    //test(CACHE_INITIAL_VALUE = false, EMIT_CHANGE_ON_RESTART = false)
    //test(CACHE_INITIAL_VALUE = true, EMIT_CHANGE_ON_RESTART = true)
    test(CACHE_INITIAL_VALUE = true/*, EMIT_CHANGE_ON_RESTART = false*/)

    def test(CACHE_INITIAL_VALUE: Boolean/*, EMIT_CHANGE_ON_RESTART: Boolean*/): Unit = {

      withClue(s"Test with cacheInitialValue=$CACHE_INITIAL_VALUE" /* + s"emitChangeOnRestart=$EMIT_CHANGE_ON_RESTART"*/) {

        implicit val testOwner: TestableOwner = new TestableOwner

        val effects = mutable.Buffer[Effect[Int]]()
        val calculations = mutable.Buffer[Calculation[Int]]()

        var downInitial = 0

        val $var = Var(1)

        val upSignal = $var
          .signal
          .setDisplayName("varSignal")
          .map(identity)
          .setDisplayName("varSignal.map(identity)")
          .map(Calculation.log("up", calculations))
          .setDisplayName("varSignal.map(identity).map")

        val changesStream = upSignal.changes //(if (EMIT_CHANGE_ON_RESTART) upSignal.changesEmitChangeOnRestart else upSignal.changes)
          .setDisplayName("upSignal.changes")
          .map(Calculation.log("changes", calculations))
          .setDisplayName("upSignal.changes.map")

        val downSignal = changesStream
          .startWith(downInitial, cacheInitialValue = CACHE_INITIAL_VALUE)
          .setDisplayName("changes.startWith")
          .map(Calculation.log("down", calculations))
          .setDisplayName("downSignal")

        val upObs = Observer(Effect.log("up-obs", effects))
        val changesObs = Observer(Effect.log("changes-obs", effects))
        val downObs = Observer[Int](v => {
          Effect.log("down-obs", effects)(v)
        })

        // --

        upSignal.addObserver(upObs)

        val downSub1 = downSignal.addObserver(downObs)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 1),
          Calculation("down", 0)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 1),
          Effect("down-obs", 0),
        )
        calculations.clear()
        effects.clear()

        // --

        downSub1.kill()

        downInitial = 10

        val downSub2 = downSignal.addObserver(downObs)

        if (CACHE_INITIAL_VALUE) {
          calculations shouldBe mutable.Buffer(
            Calculation("down", 0)
          )
          effects shouldBe mutable.Buffer(
            Effect("down-obs", 0),
          )
        } else {
          calculations shouldBe mutable.Buffer(
            Calculation("down", 10)
          )
          effects shouldBe mutable.Buffer(
            Effect("down-obs", 10),
          )
        }

        calculations.clear()
        effects.clear()

        // --

        $var.set(2)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 2),
          Calculation("changes", 2),
          Calculation("down", 2)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 2),
          Effect("down-obs", 2)
        )
        calculations.clear()
        effects.clear()

        // -- again, because there is no _.distinct now

        $var.set(2)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 2),
          Calculation("changes", 2),
          Calculation("down", 2)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 2),
          Effect("down-obs", 2)
        )
        calculations.clear()
        effects.clear()

        // --

        downSub2.kill()

        downInitial = 20

        val downSub3 = downSignal.addObserver(downObs)

        calculations shouldBe mutable.Buffer(
          Calculation("down", 2)
        )
        effects shouldBe mutable.Buffer(
          Effect("down-obs", 2)
        )

        calculations.clear()
        effects.clear()

        // --

        downSub3.kill()

        $var.set(3)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 3)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 3)
        )
        calculations.clear()
        effects.clear()

        // --

        downSignal.addObserver(downObs)

        //if (EMIT_CHANGE_ON_RESTART) {
        //  // Emitting `2` is undesirable here, which is why `emitChangeOnRestart` is off by default.
        //  // It would be great if we could get only `3`, but it's not possible to fetch this without
        //  // processing the event through the streams, because it can have a) asynchronous and
        //  // b) filtering components to it, and there's no way for us to simulate that without actually
        //  // running that logic. We can only do this for signals because signals always have a current
        //  // value and are always synchronous.
        //
        //  calculations shouldBe mutable.Buffer(
        //    Calculation("down", 2),
        //    Calculation("changes", 3),
        //    Calculation("down", 3)
        //  )
        //  effects shouldBe mutable.Buffer(
        //    Effect("down-obs", 2),
        //    Effect("down-obs", 3)
        //  )
        //} else {
          // The signal re-starts with an old value because it can't pull a fresh value from the streams
          calculations shouldBe mutable.Buffer(
            Calculation("down", 2)
          )
          effects shouldBe mutable.Buffer(
            Effect("down-obs", 2),
          )
        //}

        calculations.clear()
        effects.clear()

      }
    }
  }

  it("ChangesStream / StartWith potential glitch") {

    // #Note originally this was intended to test emitChangeOnRestart parameter of .changes,
    //  but i couldn't make that option glitch-free so we had to leave it out for now.
    //  There's a long comment about this in ChangesStream

    // #TODO Because of the above, I feel like this test is now pretty redundant...
    //  Very similar to the "ChangesStream & startWith" test above

    implicit val testOwner: TestableOwner = new TestableOwner

    val log = mutable.Buffer[String]()

    val $v = Var(1)

    def $changes = $v
      .signal.setDisplayName("VarSignal")
      .changes.setDisplayName("VarSignal.changes")

    val $isPositive = $changes.map { num =>
      val isPositive = num > 0
      log += s"$num isPositive = $isPositive"
      isPositive
    }.setDisplayName("IsPositive")

    val $isEven = $changes.map { num =>
      val isEven = num % 2 == 0
      log += s"$num isEven = $isEven"
      isEven
    }.setDisplayName("IsEven")

    val $combined = $changes.combineWithFn($isPositive, $isEven) { (num, isPositive, isEven) =>
      log += s"$num isPositive = $isPositive, isEven = $isEven"
      (isPositive, isEven)
    }.setDisplayName("Combined")

    val $result = $combined.startWith(0).setDisplayName("Result")

    val sub1 = $result.addObserver(Observer.empty)

    log.toList shouldBe Nil

    // --

    $v.set(2)

    log.toList shouldBe List(
      "2 isPositive = true",
      "2 isEven = true",
      "2 isPositive = true, isEven = true"
    )
    log.clear()

    // --

    $v.set(3)

    log.toList shouldBe List(
      "3 isPositive = true",
      "3 isEven = false",
      "3 isPositive = true, isEven = false"
    )
    log.clear()

    // --

    sub1.kill()

    $v.set(-4)

    log.toList shouldBe List()

    // --

    $combined.addObserver(Observer.empty)

    log.toList shouldBe Nil

    // --

    $v.set(-6)

    log.toList shouldBe List(
      "-6 isPositive = false",
      "-6 isEven = true",
      "-6 isPositive = false, isEven = true"
    )
    log.clear()

  }


  it("CombineEventStreamN") {

    implicit val testOwner: TestableOwner = new TestableOwner

    case class T1(v: Int)
    case class T2(v: Int)

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()

    val combinedStream = EventStream.combine(bus1, bus2)

    val effects = mutable.Buffer[(T1, T2)]()

    val observer = Observer[(T1, T2)](effects += _)

    // --

    effects.toList shouldBe Nil

    // --

    val sub1 = combinedStream.addObserver(observer)

    effects.toList shouldBe Nil

    // --

    bus1.writer.onNext(T1(0))

    effects.toList shouldBe Nil

    // --

    bus2.writer.onNext(T2(0))

    effects.toList shouldEqual List(
      (T1(0), T2(0))
    )
    effects.clear()

    // --

    bus2.writer.onNext(T2(1))

    effects.toList shouldEqual List(
      (T1(0), T2(1))
    )
    effects.clear()

    // --

    bus1.writer.onNext(T1(10))

    effects.toList shouldEqual List(
      (T1(10), T2(1))
    )
    effects.clear()

    // --

    sub1.kill()

    bus2.writer.onNext(T2(2))

    effects.toList shouldEqual Nil

    // --

    combinedStream.addObserver(observer)

    effects.toList shouldEqual Nil

    // --

    bus1.writer.onNext(T1(20))

    effects.toList shouldEqual List(
      (T1(20), T2(1))
    )
    effects.clear()

    // --

    bus2.writer.onNext(T2(3))

    effects.toList shouldEqual List(
      (T1(20), T2(3))
    )
    effects.clear()

  }

  it("CombineSignalN") {

    implicit val testOwner: TestableOwner = new TestableOwner

    case class T1(v: Int)
    case class T2(v: Int)

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[(T1, T2)]()

    val $var1 = Var(T1(0))
    val $var2 = Var(T2(0))

    val combinedSignal = $var1
      .signal
      .setDisplayName("var1.signal")
      .map { t =>
        calculations += Calculation("signal1", t.v)
        t
      }
      .setDisplayName("var1.signal.map")
      .combineWith(
        $var2
          .signal
          .setDisplayName("var2.signal")
          .map { t =>
            calculations += Calculation("signal2", t.v)
            t
          }
          .setDisplayName("var2.signal.map")
      )
      .setDisplayName("combineSignal")

    val observer = Observer[(T1, T2)](effects += _)

    // --

    calculations.toList shouldBe Nil
    effects.toList shouldBe Nil

    // --

    val sub1 = combinedSignal.addObserver(observer)

    calculations.toList shouldBe List(
      Calculation("signal1", 0),
      Calculation("signal2", 0),
    )
    effects.toList shouldBe List(
      (T1(0), T2(0))
    )

    calculations.clear()
    effects.clear()


    // --

    $var1.writer.onNext(T1(1))

    calculations.toList shouldBe List(
      Calculation("signal1", 1)
    )
    effects.toList shouldBe List(
      (T1(1), T2(0))
    )

    calculations.clear()
    effects.clear()

    // --

    $var2.writer.onNext(T2(2))

    calculations.toList shouldBe List(
      Calculation("signal2", 2)
    )
    effects.toList shouldBe List(
      (T1(1), T2(2))
    )
    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    $var2.writer.onNext(T2(3))

    calculations.toList shouldBe Nil
    effects.toList shouldBe Nil

    // --

    combinedSignal.addObserver(observer)

    calculations.toList shouldBe List(
      Calculation("signal1", 1),
      Calculation("signal2", 3)
    )
    effects.toList shouldBe List(
      (T1(1), T2(3))
    )

    calculations.clear()
    effects.clear()

    // --


    $var1.writer.onNext(T1(10))

    calculations.toList shouldBe List(
      Calculation("signal1", 10)
    )
    effects.toList shouldEqual List(
      (T1(10), T2(3))
    )

    calculations.clear()
    effects.clear()

  }

  it("MapSignal pull") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val $var = Var(1)

    val signal = $var
      .signal
      .map(Calculation.log("signal", calculations))

    val signal_x10 = signal
      .map(_ * 10)
      .map(Calculation.log("signal_x10", calculations))

    val obs = Observer(Effect.log("obs", effects))
    val obs_x10 = Observer(Effect.log("obs_x10", effects))

    // --

    signal.addObserver(obs)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )
    calculations.clear()
    effects.clear()

    // --

    val sub1_x10 = signal_x10.addObserver(obs_x10)

    calculations shouldBe mutable.Buffer(
      Calculation("signal_x10", 10)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs_x10", 10)
    )

    calculations.clear()
    effects.clear()

    // --

    $var.set(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 2),
      Calculation("signal_x10", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 2),
      Effect("obs_x10", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    sub1_x10.kill()

    $var.set(3)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )
    calculations.clear()
    effects.clear()

    // --

    signal_x10.addObserver(obs_x10)

    calculations shouldBe mutable.Buffer(
      Calculation("signal_x10", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs_x10", 30)
    )
    calculations.clear()
    effects.clear()

  }

  it("Flattened signals pull check") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[String]]()

    val outerBus = new EventBus[Int].setDisplayName("OuterBus")

    val smallSignal = EventStream
      .fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true)
      .setDisplayName("SmallSeqStream")
      .startWith("small-0")
      .setDisplayName("SmallSignal")

    val bigSignal = EventStream
      .fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true)
      .setDisplayName("BigSeqStream")
      .startWith("big-0")
      .setDisplayName("BigSignal")

    val flatSignal = outerBus
      .events
      .startWith(0)
      .setDisplayName("OuterBus.startWith")
      .map {
        case i if i >= 10 => bigSignal
        case _ => smallSignal
      }
      .setDisplayName("MetaSignal")
      .flatten
      .setDisplayName("FlatSignal")
      .map(Calculation.log("flat", calculations))
      .setDisplayName("FlatSignal--LOG")

    // --

    flatSignal.addObserver(Observer.empty)

    assert(calculations.toList == List(
      Calculation("flat", "small-0"),
      Calculation("flat", "small-1"),
      Calculation("flat", "small-2"),
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(1)

    assert(calculations.toList == List(
      Calculation("flat", "small-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(2)

    assert(calculations.toList == List(
      Calculation("flat", "small-2")
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

    outerBus.writer.onNext(11)

    assert(calculations.toList == List(
      Calculation("flat", "big-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(5) // #Note switch back to small

    assert(calculations.toList == List(
      Calculation("flat", "small-2") // Restore current value of small signal
    ))

    calculations.clear()

  }

  it("Flattened signals pull check 2") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[String]]()

    // It's important that we reuse the exact same references to inner signals to check the logic
    // - fromSeq streams are used to ensure that onStart isn't called extraneously
    // - bus.events streams are used to ensure that onStop isn't called extraneously

    val outerBus = new EventBus[Int].setDisplayName("OuterBus")

    val smallBus = new EventBus[String].setDisplayName("SmallBus")

    val bigBus = new EventBus[String].setDisplayName("BigBus")

    val smallSignal = EventStream.merge(
      smallBus.events,
      EventStream.fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true).setDisplayName("SmallSeqStream")
    ).setDisplayName("SmallMergeStream").startWith("small-0").setDisplayName("SmallSignal")

    val bigSignal = EventStream.merge(
      bigBus.events,
      EventStream.fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true).setDisplayName("BigSeqStream")
    ).setDisplayName("BigMergeStream").startWith("big-0").setDisplayName("BigSignal")

    val flatSignal = outerBus.events.startWith(0).setDisplayName("OuterBus.startWith").flatMap {
      case i if i >= 10 => bigSignal
      case _ => smallSignal
    }.setDisplayName("FlatSignal").map(Calculation.log("flat", calculations)).setDisplayName("FlatSignal--LOG")

    // --

    flatSignal.addObserver(Observer.empty)

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

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-0")
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(2)

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-1")
    ))

    calculations.clear()

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

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-1")
    ))

    calculations.clear()

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
}
