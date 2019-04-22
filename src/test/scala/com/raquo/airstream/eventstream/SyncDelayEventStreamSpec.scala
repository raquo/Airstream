package com.raquo.airstream.eventstream

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.signal.Var
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable

class SyncDelayEventStreamSpec extends FunSpec with Matchers {

  it("emits after designated stream") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val var1 = Var(1)
    val var2 = Var(100)

    val stream1 = var1.signal.changes.map(Calculation.log("stream1", calculations))
    val stream2 = var2.signal.changes.map(Calculation.log("stream2", calculations))
    val stream1synced = stream1.delaySync(after = stream2).map(Calculation.log("stream1-synced", calculations))

    // --

    stream1synced.addObserver(Observer(effects += Effect("stream1-synced-obs", _)))
    stream2.addObserver(Observer(effects += Effect("stream2-obs", _)))

    // We're testing the order of calculations / effects achieved by delaySync

    Var.set(
      var1 -> 2,
      var2 -> 200
    )

    calculations shouldEqual mutable.Buffer(
      Calculation("stream1", 2),
      Calculation("stream2", 200),
      Calculation("stream1-synced", 2)
    )
    effects shouldEqual mutable.Buffer(
      Effect("stream2-obs", 200),
      Effect("stream1-synced-obs", 2)
    )

    calculations.clear()
    effects.clear()

    // --

    Var.set(
      var1 -> 3,
      var2 -> 300
    )

    calculations shouldEqual mutable.Buffer(
      Calculation("stream1", 3),
      Calculation("stream2", 300),
      Calculation("stream1-synced", 3)
    )
    effects shouldEqual mutable.Buffer(
      Effect("stream2-obs", 300),
      Effect("stream1-synced-obs", 3)
    )

    calculations.clear()
    effects.clear()

    // --

    // We don't require stream2 to emit in order for stream1 to emit

    var1.set(4)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream1", 4),
      Calculation("stream1-synced", 4)
    )
    effects shouldEqual mutable.Buffer(
      Effect("stream1-synced-obs", 4)
    )

    calculations.clear()
    effects.clear()

    // --

    var2.set(400)

    calculations shouldEqual mutable.Buffer(
      Calculation("stream2", 400)
    )
    effects shouldEqual mutable.Buffer(
      Effect("stream2-obs", 400)
    )

    calculations.clear()
    effects.clear()
  }
}
