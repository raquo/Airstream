package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class CombineSeqSignalSpec extends UnitSpec {

  it("should work as expected") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val vars = (1 to 10).map(Var(_))
    val seqSignal = Signal.combineSeq(vars.map(_.signal))

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val observer = Observer[Seq[Int]](effects += Effect("combined", _))

    // --

    effects.toList.shouldBeEmpty

    // --

    val subscription = seqSignal.addObserver(observer)

    // --

    effects.toList shouldBe List(
      Effect("combined", (1 to 10)),
    )

    // --

    for (iteration <- 0 until 10) {
      for (signalToUpdate <- vars.indices) {
        effects.clear()
        vars(signalToUpdate).update(_ + 1)
        effects.toList shouldBe (List(
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
        ))
      }
    }
    subscription.kill()
  }

}
