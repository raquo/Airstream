package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Transaction}
import com.raquo.airstream.fixtures.TestableOwner
import com.raquo.airstream.state.Var
import com.raquo.airstream.util.FeatureFlags

import scala.annotation.nowarn
import scala.collection.mutable

/** Demonstrates a dependency-contract break in the pre-fix start scheduling, and
  * that [[FeatureFlags.V18_TRX_ONSTART_FIX_144]] fixes it. See
  * https://github.com/raquo/Airstream/issues/144.
  *
  * Airstream's ordering contract: an observable never emits before the parent it
  * derives from - a child has a strictly higher topoRank, and within a
  * transaction, a parent's (synchronous) external observers run before any child
  * `SyncObservable` (which is deferred to the end of the transaction by
  * topoRank). So parent-before-child holds WITHIN a transaction.
  *
  * That contract can only break ACROSS transactions - if a child's emission is
  * scheduled in an EARLIER transaction than its parent's. Pre-fix, a custom
  * source (`fromValue`) started inside a shared start (a Laminar mount) deferred
  * its single emission into a LATER transaction, while a `signal.changes`
  * re-sync started alongside it went through the EARLIER shared-start batch. When
  * the child samples the (still-unfired) custom source via `withCurrentValueOf`,
  * it fires first AND reads a stale value.
  *
  * `combine` is immune (it waits for all parents, so it can't outrun the slowest
  * one). `withCurrentValueOf` (sampleCombine) is not: it fires when the SAMPLING
  * stream fires and reads the sampled parent's CURRENT value.
  */
class SampleCombineStartOrderSpec extends UnitSpec {

  private def withFix144[T](enabled: Boolean)(body: => T): T = {
    val original: Boolean = {
      FeatureFlags.V18_TRX_ONSTART_FIX_144: @nowarn("msg=deprecated")
    }
    (FeatureFlags.V18_TRX_ONSTART_FIX_144 = enabled): @nowarn("msg=deprecated")
    try {
      body
    } finally {
      (FeatureFlags.V18_TRX_ONSTART_FIX_144 = original): @nowarn("msg=deprecated")
    }
  }

  it("withCurrentValueOf must not emit before (or sample a stale value from) its sampled parent on start") {
    for (fixEnabled <- Seq(true, false)) withFix144(fixEnabled) {
      withClue(s"[V18_TRX_ONSTART_FIX_144 = $fixEnabled] ") {
        val log = mutable.Buffer[String]()

        // Keep the sampling signal's source permanently alive so it retains
        // its lastUpdateId across the warm-up stop below.
        val liveOwner = new TestableOwner
        val w = Var(0)
        w.signal.foreach(_ => ())(liveOwner)

        // `sampler` re-syncs via the EARLY shared-start batch on restart.
        val sampler = w.signal.updates
        val warmUp = new TestableOwner
        sampler.foreach(_ => ())(warmUp)
        warmUp.killSubscriptions() // stop it, so restart triggers a re-sync
        w.set(5) // give it a pending update to re-emit on restart

        // `foo` is a custom source and a PARENT of `combined` (via `fooSignal`).
        // topoRank(foo) < topoRank(combined), so `foo` must emit first.
        val foo = EventStream.fromValue(100)
        val fooSignal = foo.startWith(0)
        val combined = sampler.withCurrentValueOf(fooSignal)

        assert(combined.debugTopoRank > foo.debugTopoRank)

        val owner = new TestableOwner

        // Mimics a Laminar mount: both subscriptions activate in one shared start.
        Transaction.onStart.shared {
          combined.foreach(t => log += s"combined:$t")(owner)
          foo.foreach(v => log += s"foo:$v")(owner)
        }

        val expected =
          if (fixEnabled) {
            // Fixed: parent `foo` emits first (single-event custom source now
            // shares the start batch, ordered before the child by topoRank), and
            // `combined` samples the FRESH 100.
            List("foo:100", "combined:(5,100)")
          } else {
            // Pre-fix contract break: `combined` (higher topoRank) emits BEFORE
            // its parent `foo`, sampling the STALE 0 - `foo`'s 100 is still in a
            // later transaction.
            List("combined:(5,0)", "foo:100")
          }

        log.toList shouldBe expected
      }
    }
  }

  it("sampleCombine (withCurrentValueOf) samples the fresh value only when fixed (or pre-started)") {
    for { fixEnabled <- Seq(true, false); preStartSampled <- List(false, true) } {
      withFix144(fixEnabled) {
        withClue(s"[fix=$fixEnabled, preStartSampled=$preStartSampled] ") {
          val effects = mutable.Buffer[(Int, Int)]()
          val owner = new TestableOwner
          val sampling = EventStream.fromValue(10)
          val sampled = EventStream.fromValue(20).startWith(0)

          Transaction.onStart.shared {
            if (preStartSampled) {
              sampled.foreach(_ => ())(owner)
            }
            sampling.withCurrentValueOf(sampled).foreach(effects += _)(owner)
          }

          val expected =
            if (fixEnabled || preStartSampled) {
              // Fresh sample: the sampled signal is already at 20 when sampling
              // fires 10 (same transaction under the fix, or already-propagated
              // when pre-started).
              List((10, 20))
            } else {
              // Pre-fix glitch: sampling's 10 fires in an earlier transaction
              // than the sampled's 20, so the still-initial value 0 is sampled.
              List((10, 0))
            }

          effects.toList shouldBe expected
        }
      }
    }
  }
}
