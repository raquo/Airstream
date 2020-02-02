package com.raquo.airstream.observers

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, TestableOwner}

import scala.collection.mutable
import scala.util.Success

class SignalViewerSpec extends UnitSpec {

  it("SignalViewer") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()

    val bus = new EventBus[Int]

    val signal = bus.events
      .map(Calculation.log("bus", calculations))
      .map(_ * 10)
      .toSignal(initial = -1)
      .map(Calculation.log("signal", calculations))

    // --

    bus.writer.onNext(1)

    calculations shouldEqual mutable.Buffer()

    // --

    val signalViewer = signal.observe

    calculations shouldEqual mutable.Buffer(Calculation("signal", -1))
    calculations.clear()

    // --

    signalViewer.now() shouldEqual -1
    signalViewer.tryNow() shouldEqual Success(-1)

    calculations shouldEqual mutable.Buffer()

    // --

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(Calculation("bus", 2), Calculation("signal", 20))
    calculations.clear()

    signalViewer.now() shouldEqual 20

    calculations shouldEqual mutable.Buffer()

    // --

    signalViewer.kill()
    bus.writer.onNext(3)

    signalViewer.now() shouldEqual 20

    calculations shouldEqual mutable.Buffer()
  }

}
