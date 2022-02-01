package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}

import scala.collection.mutable

class CombineSeqStreamSpec extends UnitSpec {

  it("should work as expected") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val numStreams = 10

    val buses = (1 to numStreams).map(_ => new EventBus[Int])
    val seqStream = EventStream.combineSeq(buses.map(_.events))

    val effects = mutable.Buffer[Effect[Seq[Int]]]()

    val observer = Observer[Seq[Int]](effects += Effect("combined", _))

    // --

    effects.shouldBeEmpty

    // --

    val subscription = seqStream.addObserver(observer)

    // --

    effects.shouldBeEmpty

    // --

    val numIterations = 10
    for (iteration <- 1 to numIterations) {
      for (streamToEmitFrom <- buses.indices) {
        effects.clear()
        buses(streamToEmitFrom).writer.onNext(iteration)
        if (iteration == 1) {
          if (streamToEmitFrom == numStreams-1) {
            effects.toList shouldBe List(
              Effect("combined",
                buses.indices.map( _ => iteration)
              )
            )
          } else {
            effects.shouldBeEmpty
          }
        } else {
          effects.toList shouldBe (List(
            Effect("combined",
              buses.indices.map { index =>
                if (index > streamToEmitFrom) {
                  iteration - 1
                } else {
                  iteration
                }
              }
            )
          ))
        }
      }
    }
    subscription.kill()
  }

}
