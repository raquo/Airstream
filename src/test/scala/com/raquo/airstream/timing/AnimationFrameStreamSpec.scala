package com.raquo.airstream.timing

import com.raquo.airstream.AsyncUnitSpec
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalajs.dom
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle

class AnimationFrameStreamSpec extends AsyncUnitSpec with BeforeAndAfter {

  implicit val owner: TestableOwner = new TestableOwner

  private val effects = mutable.Buffer[Effect[Int]]()
  private val tsEffects = mutable.Buffer[Effect[(Int, Double)]]()

  private val obs1 = Observer[Int](effects += Effect("obs1", _))
  private val obsTs = Observer[(Int, Double)](tsEffects += Effect("obsTs", _))

  // #TODO[Test] jsdom does not provide requestAnimationFrame unless it is created with `pretendToBeVisual`
  //  (see https://github.com/jsdom/jsdom#pretendtobevisual), so we install a simple setTimeout-based
  //  polyfill here. It lets us control animation frame timing deterministically in tests.
  private val rafTimeouts = mutable.Map[Int, SetTimeoutHandle]()
  private var nextRafId = 0

  private def installRafPolyfill(): Unit = {
    val rafFn: js.Function1[js.Function1[Double, Any], Int] = { callback =>
      nextRafId += 1
      val id = nextRafId
      rafTimeouts.put(id, js.timers.setTimeout(16.0) {
        rafTimeouts.remove(id)
        callback(js.Date.now())
      })
      id
    }

    val cancelRafFn: js.Function1[Int, Unit] = { handle =>
      rafTimeouts.remove(handle).foreach(js.timers.clearTimeout)
    }

    val window = dom.window.asInstanceOf[js.Dynamic]
    window.requestAnimationFrame = rafFn
    window.cancelAnimationFrame = cancelRafFn
  }

  before {
    owner.killSubscriptions()
    effects.clear()
    tsEffects.clear()
    rafTimeouts.clear()
    installRafPolyfill()
  }

  it("emits the last event of each animation frame") {
    val (source, emit) = EventStream.withCallback[Int]
    val stream = source.throttleWithAnimationFrame

    stream.addObserver(obs1)

    for {
      _ <- delay {
        emit(1)
        assert(effects.isEmpty)
      }

      _ <- delay(20) {
        effects.toList shouldBe List(Effect("obs1", 1))
        effects.clear()

        // Multiple events in the same frame are coalesced into the last one
        emit(2)
        emit(3)
        emit(4)

        assert(effects.isEmpty)
      }

      _ <- delay(20) {
        effects.toList shouldBe List(Effect("obs1", 4))
        effects.clear()
      }
    } yield {
      succeed
    }
  }

  it("emits the event together with its animation frame timestamp") {
    val (source, emit) = EventStream.withCallback[Int]
    val stream = source.throttleWithAnimationFrameWithTs

    stream.addObserver(obsTs)

    for {
      _ <- delay {
        emit(42)
        assert(tsEffects.isEmpty)
      }

      _ <- delay(20) {
        tsEffects.map(_.value._1).toList shouldBe List(42)
        assert(tsEffects.head.value._2 > 0)
        tsEffects.clear()
      }
    } yield {
      succeed
    }
  }

  it("forgets pending events and cancels pending frames on stop") {
    val (source, emit) = EventStream.withCallback[Int]
    val stream = source.throttleWithAnimationFrame

    val sub = stream.addObserver(obs1)

    for {
      _ <- delay {
        emit(1)
        assert(effects.isEmpty)

        sub.kill() // stop before the frame fires, cancelling the pending frame

        assert(rafTimeouts.isEmpty)
      }

      _ <- delay(30) {
        assert(effects.isEmpty)
      }
    } yield {
      succeed
    }
  }
}
