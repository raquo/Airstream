package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class ThrottleStreamSpec extends AsyncUnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val effects = mutable.Buffer[Effect[Int]]()

  private val obs1 = Observer[Int](effects += Effect("obs1", _))

  before {
    owner.killSubscriptions()
    effects.clear()
  }

  it("throttling with leading = true") {
    val (source, emit) = EventStream.withCallback[Int]
    val throttled = source.throttle(100, leading = true)

    throttled.addObserver(obs1)

    for {
      _ <- delay {
        emit(1)
        assert(effects.isEmpty)
      }

      _ <- delay(50) {
        assert(effects.toList == List(Effect("obs1", 1)))
        effects.clear()

        emit(2)
        emit(3)

        assert(effects.isEmpty)
      }

      _ <- delay(100) {
        assert(effects.toList == List(Effect("obs1", 3)))
        effects.clear()

        emit(4)

        assert(effects.isEmpty)
      }

      _ <- delay(30) {
        emit(5)
      }

      _ <- delay(30) {
        emit(6)
      }

      _ <- delay(30) {
        emit(7)
      }

      _ <- delay(30) {
        emit(8)
      }

      _ <- delay(30) {
        emit(9)
      }

      _ <- delay(30) {
        emit(10)
      }

      _ <- delay(30) {
        emit(11)
      }

      _ <- delay(100) {
        // Timing will probably get screwed up in test env anyway, so we're not asserting individual events
        // Point is, throttle operator should be throttling, not emitting every event, and not waiting for
        // parent to stop emitting like debounce
        assert(effects.length >= 2)
        assert(effects.length <= 3)
        effects.clear()
      }

      end <- delay(100) { // a bit extra margin for the last check just to be sure that we caught any events
        assert(effects.isEmpty)
      }

    } yield {
      end
    }
  }

  it("throttling with leading = false") {
    val (source, emit) = EventStream.withCallback[Int]
    val throttled = source.throttle(100, leading = false)

    throttled.addObserver(obs1)

    for {
      _ <- delay {
        emit(1)
      }

      _ <- delay(50) {
        assert(effects.isEmpty)

        emit(2)
        emit(3)

        assert(effects.isEmpty)
      }

      _ <- delay(100) {
        assert(effects.toList == List(Effect("obs1", 3)))
        effects.clear()

        emit(4)

        assert(effects.isEmpty)
      }

      _ <- delay(30) {
        emit(5)
      }

      _ <- delay(30) {
        emit(6)
      }

      _ <- delay(30) {
        emit(7)
      }

      _ <- delay(30) {
        emit(8)
      }

      _ <- delay(30) {
        emit(9)
      }

      _ <- delay(30) {
        emit(10)
      }

      _ <- delay(30) {
        emit(11)
      }

      _ <- delay(100) {
        // Timing will probably get screwed up in test env anyway, so we're not asserting individual events
        // Point is, throttle operator should be throttling, not emitting every event, and not waiting for
        // parent to stop emitting like debounce
        assert(effects.length >= 2)
        assert(effects.length <= 3)
        effects.clear()
      }

      end <- delay(100) { // a bit extra margin for the last check just to be sure that we caught any events
        assert(effects.isEmpty)
      }

    } yield {
      end
    }
  }

}
