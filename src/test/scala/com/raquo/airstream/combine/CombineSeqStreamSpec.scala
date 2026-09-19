package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.core.AirstreamError.CombinedError
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class CombineSeqStreamSpec extends UnitSpec {

  it("counts each input once, including failures, across restarts") {
    implicit val owner: TestableOwner = new TestableOwner
    val first = EventBus[Int]()
    val second = EventBus[Int]()
    val combined = EventStream.combineSeq(Seq(first.events, second.events))
    val effects = mutable.Buffer[Try[Seq[Int]]]()
    val observer = Observer.fromTry[Seq[Int]](effects += _)
    val error = new Exception("first input failed")

    combined.addObserver(observer)
    first.emitTry(Failure(error))
    first.emitTry(Failure(error))
    effects.shouldBeEmpty
    owner.killSubscriptions()

    combined.addObserver(observer)
    second.emit(2)
    effects.toList shouldBe List(Failure(CombinedError(List(Some(error), None))))
    first.emit(3)
    effects.last shouldBe Success(Seq(3, 2))
    owner.killSubscriptions()

    combined.addObserver(observer)
    second.emit(4)
    effects.last shouldBe Success(Seq(3, 4))
    effects.size shouldBe 3
    owner.killSubscriptions()
  }

  it("counts duplicate parent positions and combines a batch only once") {
    implicit val owner: TestableOwner = new TestableOwner
    val first = EventBus[Int]()
    val second = EventBus[Int]()
    val effects = mutable.Buffer[Seq[Int]]()
    EventStream.combineSeq(Seq(first.events, first.events, second.events)).foreach(effects += _)

    first.emit(1)
    first.emit(2)
    effects.shouldBeEmpty
    EventBus.emit(first -> 3, second -> 4)
    effects.toList shouldBe List(Seq(3, 3, 4))
    EventBus.emit(first -> 5, second -> 6)
    effects.toList shouldBe List(Seq(3, 3, 4), Seq(5, 5, 6))
    owner.killSubscriptions()
  }

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
          if (streamToEmitFrom == numStreams - 1) {
            effects.toList shouldBe List(
              Effect("combined",
                buses.indices.map(_ => iteration)
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
