package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.flatten.FlattenStrategy.SwitchStreamStrategy
import com.raquo.airstream.state.Var

import scala.collection.mutable
import scala.util.{Success, Try}

class SwitchStreamSpec extends UnitSpec {

  it("EventStream: mirrors last emitted stream, but only if subscribed") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val metaBus = new EventBus[EventStream[Int]]

    // Create 4 test buses and add logging to their streams
    val sourceBuses = (1 to 4).map(_ => new EventBus[Int])
    val sourceStreams = sourceBuses.zipWithIndex.map {
      case (bus, index) => bus.events.map(Calculation.log(s"source-$index", calculations))
    }

    val latestNumberS = metaBus.events.flattenSwitch // SwitchStreamStrategy is the default (provided implicitly)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenStream = latestNumberS
      .map(Calculation.log("flattened", calculations))

    val subFlatten = flattenStream.addObserver(flattenObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses.foreach(_.writer.onNext(-1))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    metaBus.writer.onNext(sourceStreams(0))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses(0).writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("source-0", 1),
      Calculation("flattened", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    metaBus.writer.onNext(sourceStreams(1))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses(1).writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("source-1", 2),
      Calculation("flattened", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    metaBus.writer.onNext(sourceStreams(2))

    val sourceStream2Observer = Observer[Int](effects += Effect("source-2-obs", _))

    sourceStreams(2).addObserver(sourceStream2Observer)
    subFlatten.kill()

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses(2).writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    flattenStream.addObserver(flattenObserver) // re-activate flattened stream

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    // flatten stream remembers the stream it was following even after restart
    sourceBuses(2).writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 4),
      Calculation("flattened", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 4),
      Effect("flattened-obs", 4)
    )

    calculations.clear()
    effects.clear()
  }

  it("Signal: mirrors last emitted stream, but only if subscribed") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    // Create 4 test buses and add logging to their streams
    val sourceBuses = (1 to 4).map(_ => new EventBus[Int])
    val sourceStreams = sourceBuses.zipWithIndex.map {
      case (bus, index) => bus.events.map(Calculation.log(s"source-$index", calculations))
    }

    val metaVar = Var[EventStream[Int]](sourceStreams(0))

    val latestNumberS = metaVar.signal.flattenSwitch(SwitchStreamStrategy)

    val flattenObserver = Observer[Int](effects += Effect("flattened-obs", _))

    val flattenStream = latestNumberS
      .map(Calculation.log("flattened", calculations))

    val subFlatten1 = flattenStream.addObserver(flattenObserver)

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses(0).writer.onNext(1)

    calculations shouldBe mutable.Buffer(
      Calculation("source-0", 1),
      Calculation("flattened", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 1)
    )

    calculations.clear()
    effects.clear()

    // --

    metaVar.writer.onNext(sourceStreams(1))

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses(1).writer.onNext(2)

    calculations shouldBe mutable.Buffer(
      Calculation("source-1", 2),
      Calculation("flattened", 2)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    metaVar.writer.onNext(sourceStreams(2))

    val sourceStream2Observer = Observer[Int](effects += Effect("source-2-obs", _))

    val sourceSub2 = sourceStreams(2).addObserver(sourceStream2Observer)
    subFlatten1.kill()

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses(2).writer.onNext(3)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    val subFlatten2 = flattenStream.addObserver(flattenObserver) // re-activate flattened stream

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // -- re-subscribing to the same stream keeps memory of last stream

    sourceBuses(2).writer.onNext(4)

    calculations shouldBe mutable.Buffer(
      Calculation("source-2", 4),
      Calculation("flattened", 4)
    )
    effects shouldBe mutable.Buffer(
      Effect("source-2-obs", 4),
      Effect("flattened-obs", 4)
    )

    calculations.clear()
    effects.clear()

    // -- re-subscribing to a new stream pulls it from parent signal

    subFlatten2.kill()
    sourceSub2.kill()

    metaVar.writer.onNext(sourceStreams(3))

    sourceBuses(1).writer.onNext(5)
    sourceBuses(3).writer.onNext(6)

    flattenStream.addObserver(flattenObserver) // re-activate flattened stream

    calculations shouldBe mutable.Buffer()
    effects shouldBe mutable.Buffer()

    // --

    sourceBuses(1).writer.onNext(7)
    sourceBuses(3).writer.onNext(8)

    calculations shouldBe mutable.Buffer(
      Calculation("source-3", 8),
      Calculation("flattened", 8)
    )
    effects shouldBe mutable.Buffer(
      Effect("flattened-obs", 8)
    )

    calculations.clear()
    effects.clear()

  }

  it("EventStream: emitting the same inner stream does not cause it to stop and re-start") {

    implicit val owner: TestableOwner = new TestableOwner

    val outerBus = new EventBus[Int]

    val calculations = mutable.Buffer[Calculation[String]]()

    // It's important that we reuse the exact same references to inner streams to check the logic
    // - fromSeq streams are used to ensure that onStart isn't called extraneously
    // - bus.events streams are used to ensure that onStop isn't called extraneously

    val smallBus = new EventBus[String]

    val smallStream = EventStream.merge(
      smallBus.events,
      EventStream.fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true)
    )

    val bigBus = new EventBus[String]

    val bigStream = EventStream.merge(
      bigBus.events,
      EventStream.fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true)
    )

    val flatStream = outerBus.events.flatMapSwitch {
      case i if i >= 10 => bigStream
      case _ => smallStream
    }.map(Calculation.log("flat", calculations))

    // --

    flatStream.addObserver(Observer.empty)

    assert(calculations.isEmpty)

    // --

    outerBus.writer.onNext(1)

    assert(calculations.toList == List(
      Calculation("flat", "small-1"),
      Calculation("flat", "small-2"),
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

    assert(calculations.isEmpty)

    // --

    smallBus.writer.onNext("small-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(10) // #Note switch to big

    assert(calculations.toList == List(
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

    assert(calculations.isEmpty)

    // --

    bigBus.writer.onNext("big-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(5) // #Note switch back to small

    assert(calculations.isEmpty) // empty because of emitOnce = true

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

  it("Signal: emitting the same inner stream does not cause it to stop and re-start") {

    implicit val owner: TestableOwner = new TestableOwner

    val outerBus = new EventBus[Int].setDisplayName("outer-bus")

    val calculations = mutable.Buffer[Calculation[String]]()

    // It's important that we reuse the exact same references to inner streams to check the logic
    // - fromSeq streams are used to ensure that onStart isn't called extraneously
    // - bus.events streams are used to ensure that onStop isn't called extraneously

    val smallBus = new EventBus[String].setDisplayName("small-bus")

    val smallStream = EventStream.merge(
      smallBus.events.setDisplayName("small-bus-events"),
      EventStream.fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true).setDisplayName("small-fromSeq")
    ).setDisplayName("small-M")

    val bigBus = new EventBus[String].setDisplayName("big-bus")

    val bigStream = EventStream.merge(
      bigBus.events.setDisplayName("big-bus-events"),
      EventStream.fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true).setDisplayName("big-fromSeq")
    ).setDisplayName("big-M")

    val flatStream = outerBus.events.setDisplayName("outer-bus-events").startWith(0).setDisplayName("outer-signal").map {
      case i if i >= 10 => bigStream
      case _ => smallStream
    }.setDisplayName("outer-meta")
      .flattenSwitch.setDisplayName("outer-flat")
      .map(Calculation.log("flat", calculations)).setDisplayName("outer-flat-map")

    // --

    val emptyObserver = Observer.empty.setDisplayName("emptyObserver")

    flatStream.addObserver(emptyObserver)

    assert(calculations.toList == List(
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

    assert(calculations.isEmpty) // Signal == filter eats this up

    // --

    smallBus.writer.onNext("small-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(2) // Signal == filter eats this up

    assert(calculations.isEmpty)

    // --

    smallBus.writer.onNext("small-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(10) // #Note switch to big

    assert(calculations.toList == List(
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

    assert(calculations.isEmpty)

    // --

    bigBus.writer.onNext("big-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(5) // #Note switch back to small

    assert(calculations.isEmpty) // empty because of emitOnce = true

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

    val intBus = new EventBus[Int]

    val intStream = intBus.events

    val brokenSignal =
      intStream
        .flatMapSwitch { num =>
          if (num < 1000) {
            smallI += 1
            intStream.map("small: " + _).setDisplayName(s"small-$smallI") // .debugLogLifecycle()
          } else {
            bigI += 1
            EventStream.fromValue("big").setDisplayName(s"inner-$bigI")
          }
        }

    brokenSignal.foreach(Effect.log("output", effects))(owner)

    def emit(v: Int): Unit = {
      Effect.log("emit", effects)(v.toString)
      intBus.emit(v)
    }

    // --

    emit(884)
    emit(887)
    emit(1018)
    emit(1141)
    emit(1142)

    effects shouldBe mutable.Buffer(
      Effect("emit", "884"),
      Effect("output", "small: 884"),
      Effect("emit", "887"),
      Effect("output", "small: 887"),
      Effect("emit", "1018"),
      Effect("output", "big"),
      Effect("emit", "1141"),
      Effect("output", "big"),
      Effect("emit", "1142"),
      Effect("output", "big")
    )
  }

  it("Switching between two streams does not cause their common ancestor to briefly stop") {

    val owner = new TestableOwner

    val effects = mutable.Buffer[Effect[_]]()

    var updateSource: Int => Unit = _ => throw new Exception("source signal has not been started yet")

    val source = EventStream.fromCustomSource[Int](
      start = (fireValue, fireError, getStartIx, getIsStarted) => {
        updateSource = fireValue
        effects += Effect("source-start", "ix-" + getStartIx())
      },
      stop = startIx => {
        effects += Effect("source-stop", "ix-" + startIx)
      }
    )

    val stream1 = source.map(_ * 10)
    val stream2 = source.map(_ * 100)

    val switch = new EventBus[Int]

    switch
      .events
      .flatMapSwitch { v =>
        effects += Effect("switch", v)
        if (v % 2 == 0) stream1 else stream2
      }
      .foreach(v => {
        effects += Effect("result", v)
      })(owner)

    assertEquals(
      effects.toList,
      Nil
    )

    switch.emit(1)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 1),
        Effect("source-start", "ix-1")
      )
    )
    effects.clear()

    // --

    updateSource(2)

    assertEquals(
      effects.toList,
      List(
        Effect("result", 200)
      )
    )
    effects.clear()

    // --

    switch.emit(2)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 2)
      )
    )
    effects.clear()

    // --

    switch.emit(3)

    assertEquals(
      effects.toList,
      List(
        Effect("switch", 3)
      )
    )
    effects.clear()

    // --

    updateSource(4)

    assertEquals(
      effects.toList,
      List(
        Effect("result", 400)
      )
    )
    effects.clear()
  }
}
