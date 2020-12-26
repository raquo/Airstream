package com.raquo.airstream.signal

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.{ Calculation, Effect, TestableOwner }

import scala.collection.mutable

class SeqSignalSpec extends UnitSpec {

  it("should work as expected") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val vars = (1 to 10).map(Var(_))
    val seqSignal = Signal.seq(vars.map(_.signal))

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val observer = Observer[Seq[Int]](effects += Effect("combined", _))

    // --

    effects.toList shouldBe empty

    // --

    val subscription = seqSignal.addObserver(observer)

    // --

    effects.toList shouldEqual List(
      Effect("combined", (1 to 10)),
    )

    // --

    for (iteration <- 0 until 10) {
      for (signalToUpdate <- vars.indices) {
        vars(signalToUpdate).update(_ + 1)
        effects.toList.last shouldEqual
          Effect("combined",
            vars.indices.map { index =>
              if (index > signalToUpdate) {
                index + 1 + // initial
                  iteration // increased in prev iterations
              } else {
                index + 1 + // initial
                  iteration + // increased in prev iterations
                  1 // increased in this iterations
              }
            }
          )
      }
    }
    subscription.kill()
  }

}
