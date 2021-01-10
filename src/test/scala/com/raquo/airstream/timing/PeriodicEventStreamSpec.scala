package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class PeriodicEventStreamSpec extends AsyncUnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val effects = mutable.Buffer[Effect[Int]]()

  private val obs1 = Observer[Int](effects += Effect("obs1", _))

  private val done = assert(true)

  before {
    owner.killSubscriptions()
    effects.clear()
  }

  it("emitInitial=true, resetOnStop=true") {
    val testInterval = 50

    val stream = EventStream.periodic(intervalMs = testInterval, emitInitial = true, resetOnStop = true)

    val sub1 = stream.addObserver(obs1)

    effects shouldEqual mutable.Buffer(Effect("obs1", 0))
    effects.clear()

    for {
      _ <- delay(2) { }
      _ <- delay(testInterval) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(testInterval) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(testInterval) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(1)
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(testInterval) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(testInterval + testInterval) {
        effects shouldEqual mutable.Buffer()
      }
      sub2 = stream.addObserver(obs1)
      _ = {
        effects shouldEqual mutable.Buffer(Effect("obs1", 0))
        effects.clear()
      }
      _ <- delay(2) { }
      _ <- delay(testInterval) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ = sub2.kill()
    } yield done
  }

  it("emitInitial=false, resetOnStop=true") {
    val stream = EventStream.periodic(intervalMs = 15, emitInitial = false, resetOnStop = true)

    val sub1 = stream.addObserver(obs1)

    effects shouldEqual mutable.Buffer()

    for {
      _ <- delay(2) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(1)
        effects shouldEqual mutable.Buffer()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(20) {
        effects shouldEqual mutable.Buffer()
      }
      _ = stream.addObserver(obs1)
      _ = {
        effects shouldEqual mutable.Buffer()
      }
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }

    } yield done
  }

  it("emitInitial=true, resetOnStop=false") {
    val stream = EventStream.periodic(intervalMs = 15, emitInitial = true, resetOnStop = false)

    val sub1 = stream.addObserver(obs1)

    effects shouldEqual mutable.Buffer(Effect("obs1", 0))
    effects.clear()

    for {
      _ <- delay(2) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(1)
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(20) {
        effects shouldEqual mutable.Buffer()
      }
      _ = stream.addObserver(obs1)
      _ = {
        effects shouldEqual mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay(2) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }

    } yield done
  }

  it("emitInitial=false, resetOnStop=false") {
    val stream = EventStream.periodic(intervalMs = 15, emitInitial = false, resetOnStop = false)

    val sub1 = stream.addObserver(obs1)

    effects shouldEqual mutable.Buffer()
    effects.clear()

    for {
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(1)
        effects shouldEqual mutable.Buffer()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ = sub1.kill()
      _ <- delay(20) {
        effects shouldEqual mutable.Buffer()
      }
      _ = stream.addObserver(obs1)
      _ = {
        effects shouldEqual mutable.Buffer()
      }
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }

    } yield done
  }

  it("dynamic interval") {
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
      emitInitial = true,
      resetOnStop = true
    )

    val sub1 = stream.addObserver(obs1)

    effects shouldEqual mutable.Buffer(Effect("obs1", 0))
    effects.clear()

    for {
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 2))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 5))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldEqual mutable.Buffer()
      }
      _ = sub1.kill()
      _ = stream.addObserver(obs1)
      _ <- delay(5) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 0))
        effects.clear()
      }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 1))
        effects.clear()
      }
      _ <- delay {
        stream.resetTo(3)
        effects shouldEqual mutable.Buffer(Effect("obs1", 3))
        effects.clear()
      }
      _ <- delay(5) { }
      _ <- delay(15) {
        effects shouldEqual mutable.Buffer() // Make sure interval is updated
      }
      _ <- delay(30 - 15) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 4))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldEqual mutable.Buffer(Effect("obs1", 5))
        effects.clear()
      }
      _ <- delay(30) {
        effects shouldEqual mutable.Buffer()
      }

    } yield done
  }

}
