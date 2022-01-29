package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, TestableOwner}

import scala.collection.mutable
import scala.util.Success

class OwnedSignalSpec extends UnitSpec {

  it("OwnedSignal") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()

    val bus = new EventBus[Int]

    val signal = bus.events
      .map(Calculation.log("bus", calculations))
      .map(_ * 10)
      .startWith(-1)
      .map(Calculation.log("signal", calculations))

    // --

    bus.writer.onNext(1)

    calculations shouldBe mutable.Buffer()

    // --

    val signalViewer = signal.observe

    calculations shouldBe mutable.Buffer(Calculation("signal", -1))
    calculations.clear()

    // --

    signalViewer.now() shouldBe -1
    signalViewer.tryNow() shouldBe Success(-1)

    calculations shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(2)

    calculations shouldBe mutable.Buffer(Calculation("bus", 2), Calculation("signal", 20))
    calculations.clear()

    signalViewer.now() shouldBe 20

    calculations shouldBe mutable.Buffer()

    // --

    signalViewer.killOriginalSubscription()
    bus.writer.onNext(3)

    signalViewer.now() shouldBe 20

    calculations shouldBe mutable.Buffer()
  }

}
