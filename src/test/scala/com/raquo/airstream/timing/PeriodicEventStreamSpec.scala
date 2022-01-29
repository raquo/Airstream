package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class PeriodicEventStreamSpec extends AsyncUnitSpec {

  private val done = assert(true)

  it("resetOnStop=true") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))

    val testInterval = 50

    val stream = EventStream.periodic(intervalMs = testInterval, resetOnStop = true)

    val sub1 = stream.addObserver(obs1)

    effects shouldBe mutable.Buffer(Effect("obs1", 0))
    effects.clear()

    for {
      _ <- delay(2) { }
      _ <- delay(testInterval) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(testInterval) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(testInterval) {
        effects shouldBe mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(1)
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(testInterval) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(testInterval + testInterval) {
        effects shouldBe mutable.Buffer()
      }
      sub2 = stream.addObserver(obs1)
      _ = {
        effects shouldBe mutable.Buffer(Effect("obs1", 0))
        effects.clear()
      }
      _ <- delay(2) { }
      _ <- delay(testInterval) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ = sub2.kill()
    } yield done
  }

  it("resetOnStop=true + drop(1)") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))

    val source = EventStream.periodic(intervalMs = 15, resetOnStop = true)
    val stream = source.drop(1, resetOnStop = true)

    val sub1 = stream.addObserver(obs1)

    effects shouldBe mutable.Buffer()

    for {
      _ <- delay(2) { }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        source.resetTo(1)
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(20) {
        effects shouldBe mutable.Buffer()
      }
      _ = stream.addObserver(obs1)
      _ = {
        effects shouldBe mutable.Buffer()
      }
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }

    } yield done
  }

  it("resetOnStop=false") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))

    val stream = EventStream.periodic(intervalMs = 15, resetOnStop = false)

    val sub1 = stream.addObserver(obs1)

    effects shouldBe mutable.Buffer(Effect("obs1", 0))
    effects.clear()

    for {
      _ <- delay(2) { }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(1)
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(20) {
        effects shouldBe mutable.Buffer()
      }
      _ = stream.addObserver(obs1)
      _ = {
        effects shouldBe mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay(2) { }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }

    } yield done
  }

  it("resetOnStop=false + drop(1)") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))

    val source = EventStream.periodic(intervalMs = 50, resetOnStop = false)
    val stream = source.drop(1, resetOnStop = true)

    val sub1 = stream.addObserver(obs1)

    effects shouldBe mutable.Buffer()
    effects.clear()

    for {
      _ <- delay(5) { }
      _ <- delay(50) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(50) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(50) {
        effects shouldBe mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        source.resetTo(1)
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(50) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(55) {
        effects shouldBe mutable.Buffer()
      }
      _ = stream.addObserver(obs1)
      _ = {
        effects shouldBe mutable.Buffer()
      }
      _ <- delay(5) { }
      _ <- delay(50) {
        effects shouldBe mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }

    } yield done
  }

  it("dynamic interval") {

    implicit val owner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))

    val stream = new PeriodicEventStream[Int](
      initial = 0,
      next = index => {
        if (index < 5) {
          val nextIndex = index + 1
          val nextInterval = if (index <= 1) 15 else 30
          Some((nextIndex, nextInterval))
        } else {
          None
        }
      },
      resetOnStop = true
    )

    val sub1 = stream.addObserver(obs1)

    effects shouldBe mutable.Buffer(Effect("obs1", 0))
    effects.clear()

    for {
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldBe mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldBe mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldBe mutable.Buffer(Effect("obs1", 5))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldBe mutable.Buffer()
      }
      _ = sub1.kill()
      _ = stream.addObserver(obs1)
      _ <- delay(5) {
        effects shouldBe mutable.Buffer(Effect("obs1", 0))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(3)
        effects shouldBe mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldBe mutable.Buffer() // Make sure interval is updated
      }
      _ <- delay(30 - 15) {
        effects shouldBe mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldBe mutable.Buffer(Effect("obs1", 5))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldBe mutable.Buffer()
      }

    } yield done
  }

}
