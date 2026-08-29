package com.raquo.airstream.combine

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Transaction}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.state.Var
import com.raquo.airstream.util.FeatureFlags

import scala.annotation.nowarn
import scala.collection.mutable

/** Covers https://github.com/raquo/Airstream/issues/144 and MattJ's Discord
  * report: when merging `EventStream.fromValue(x)` with another stream, the
  * merged emit order used to depend on the order the parents were *started* in,
  * not the order they were passed to `merge`.
  *
  * Root cause (pre-fix): a custom source (`fromValue` and friends) emitted each
  * value in `onStart` in its own `new Transaction`. Started together inside one
  * `Transaction.onStart.shared { ... }` block (as Laminar's mounting does), those
  * transactions were deferred and scheduled in start order, not topoRank order.
  *
  * The fix has two parts, both gated behind
  * [[FeatureFlags.V18_TRX_ONSTART_FIX_144]] (default on):
  *  1. A custom source that emits exactly ONE event on start now emits it via the
  *     shared-start batch (`Transaction.onStart.add`), like `signal.updates`, so
  *     simultaneous start-emissions share one transaction. Multi-event sources
  *     (e.g. `fromSeq`) stay on standalone transactions - see the fromSeq test.
  *  2. `MergeStream` breaks priority-queue ties by parent (argument) index
  *     instead of arrival order.
  *
  * Each flag-sensitive test asserts both the new (flag on) and old (flag off)
  * behaviour. `combine` is unaffected; `sampleCombine`'s start sample is - see
  * the tests at the bottom.
  */
class MergeStreamStartTransactionOrderSpec extends UnitSpec {

  /** Run `body` with the fix flag forced on/off, restoring it afterwards. */
  private def withFix144[T](enabled: Boolean)(body: => T): T = {
    val original: Boolean = FeatureFlags.V18_TRX_ONSTART_FIX_144: @nowarn("msg=deprecated")
    (FeatureFlags.V18_TRX_ONSTART_FIX_144 = enabled): @nowarn("msg=deprecated")
    try {
      body
    } finally {
      (FeatureFlags.V18_TRX_ONSTART_FIX_144 = original): @nowarn("msg=deprecated")
    }
  }

  // --- Baseline (no shared start) --------------------------------------------

  it("baseline: merge(fromValue(1), fromValue(2)) emits 1 then 2 when started fresh") {
    // Not flag-sensitive: with no pre-starting, start order == merge order, so
    // both the new (shared batch) and old (per-event transaction) schedules emit
    // 1 then 2. We assert it under both flags to pin that down.
    for (fixEnabled <- Seq(true, false)) withFix144(fixEnabled) {
      withClue(s"[V18_TRX_ONSTART_FIX_144 = $fixEnabled] ") {
        val effects = mutable.Buffer[Effect[Int]]()
        val owner = new TestableOwner

        val stream1 = EventStream.fromValue(1)
        val stream2 = EventStream.fromValue(2)

        val mergeStream = stream1.mergeWith(stream2)

        mergeStream.foreach(v => effects += Effect("merge", v))(owner)

        effects.toList shouldBe List(
          Effect("merge", 1),
          Effect("merge", 2)
        )
      }
    }
  }

  it("baseline: if stream2 is already started, merge misses its (already-emitted) value") {
    val effects = mutable.Buffer[Effect[Int]]()
    val owner = new TestableOwner

    val stream1 = EventStream.fromValue(1)
    val stream2 = EventStream.fromValue(2)

    // Starting stream2 makes it emit `2` as part of its own start (its
    // addObserver wraps starting in its own onStart.shared block, which
    // resolves right away since it isn't nested in another one). mergeStream
    // doesn't exist yet, so nobody hears it.
    stream2.foreach(_ => ())(owner)

    val mergeStream = stream1.mergeWith(stream2)

    mergeStream.foreach(v => effects += Effect("merge", v))(owner)

    // stream2 stays started, so it doesn't re-emit; only stream1's `1` is seen.
    effects.toList shouldBe List(
      Effect("merge", 1)
    )
  }

  // --- The bug: ordering depends on start order inside a shared start block ---

  it("inside onStart.shared, merge emits in merge order regardless of start order (only when fixed)") {
    // This mimics Laminar's onMountCallback / DynamicOwner activation, which
    // wraps all subscription activations of a mount into onStart.shared. We
    // start stream2 FIRST inside the block.
    for (fixEnabled <- Seq(true, false)) withFix144(fixEnabled) {
      withClue(s"[V18_TRX_ONSTART_FIX_144 = $fixEnabled] ") {
        val effects = mutable.Buffer[Effect[Int]]()
        val owner = new TestableOwner

        val stream1 = EventStream.fromValue(1)
        val stream2 = EventStream.fromValue(2)

        Transaction.onStart.shared {
          stream2.foreach(_ => ())(owner) // start stream2 first

          val mergeStream = stream1.mergeWith(stream2)

          mergeStream.foreach(v => effects += Effect("merge", v))(owner)
        }

        val expected =
          if (fixEnabled) {
            // Both land in one transaction, ordered by merge argument index:
            // 1 then 2, though stream2 started first. MattJ's report, fixed.
            List(Effect("merge", 1), Effect("merge", 2))
          } else {
            // Each in its own transaction, deferred in start order: 2 before 1.
            List(Effect("merge", 2), Effect("merge", 1))
          }

        effects.toList shouldBe expected
      }
    }
  }

  it("FIXED: reversing which stream starts first does NOT change the emit order") {
    val effects = mutable.Buffer[Effect[Int]]()
    val owner = new TestableOwner

    val stream1 = EventStream.fromValue(1)
    val stream2 = EventStream.fromValue(2)

    Transaction.onStart.shared {
      // stream1 started first: emit order is 1,2 either way here (start order
      // matches merge order). The discriminating case is the test above, where
      // stream2 starts first.
      stream1.foreach(_ => ())(owner)

      val mergeStream = stream1.mergeWith(stream2)

      mergeStream.foreach(v => effects += Effect("merge", v))(owner)
    }

    effects.toList shouldBe List(
      Effect("merge", 1),
      Effect("merge", 2)
    )
  }

  it("FIXED: without pre-starting, merge emits in merge argument order") {
    val effects = mutable.Buffer[Effect[Int]]()
    val owner = new TestableOwner

    val stream1 = EventStream.fromValue(1)
    val stream2 = EventStream.fromValue(2)

    val mergeStream = stream1.mergeWith(stream2)

    Transaction.onStart.shared {
      mergeStream.foreach(v => effects += Effect("merge", v))(owner)
    }

    effects.toList shouldBe List(
      Effect("merge", 1),
      Effect("merge", 2)
    )
  }

  // --- fromSeq: single-event batches, multi-event stays on its own trx -------

  it("fromSeq still emits all events in order when started standalone") {
    for (fixEnabled <- Seq(true, false)) withFix144(fixEnabled) {
      withClue(s"[V18_TRX_ONSTART_FIX_144 = $fixEnabled] ") {
        val effects = mutable.Buffer[Effect[Int]]()
        val owner = new TestableOwner

        EventStream.fromSeq(List(1, 2, 3)).foreach(v => effects += Effect("seq", v))(owner)

        effects.toList shouldBe List(
          Effect("seq", 1),
          Effect("seq", 2),
          Effect("seq", 3)
        )
      }
    }
  }

  it("multi-event fromSeq is NOT batched, preserving its internal event order") {
    // A custom source only batches its start-emission when it emits exactly ONE
    // event, so multi-event fromSeq stays fully on standalone transactions.
    // Batching only its first event would let its tail overtake it through the
    // deferring merge, corrupting fromSeq's order. With the fix on, stream1's `1`
    // (single, batched) leads and stream2's events follow in order; with it off,
    // everything is start-ordered, so stream2 (started first) comes before `1`.
    for (fixEnabled <- Seq(true, false)) withFix144(fixEnabled) {
      withClue(s"[V18_TRX_ONSTART_FIX_144 = $fixEnabled] ") {
        val effects = mutable.Buffer[Effect[Int]]()
        val owner = new TestableOwner

        val stream1 = EventStream.fromValue(1)
        val stream2 = EventStream.fromSeq(List(2, 20, 200))

        Transaction.onStart.shared {
          stream2.foreach(_ => ())(owner) // start the fromSeq first
          val mergeStream = stream1.mergeWith(stream2)
          mergeStream.foreach(v => effects += Effect("merge", v))(owner)
        }

        val expected =
          if (fixEnabled) {
            List(Effect("merge", 1), Effect("merge", 2), Effect("merge", 20), Effect("merge", 200))
          } else {
            List(Effect("merge", 2), Effect("merge", 20), Effect("merge", 200), Effect("merge", 1))
          }

        effects.toList shouldBe expected
      }
    }
  }

  // --- Contrast: signal.updates orders by topoRank, NOT start order ----------

  it("CONTRAST: merge of two signal.updates emits in topoRank order regardless of start order") {
    // signal.updates always emits its restart value via the shared-start batch
    // (regardless of this flag), so two merged updates land in one transaction
    // and the merge queue orders them by topoRank, not start order. The fix makes
    // single-event custom sources behave the same way. Here updates2 has a HIGHER
    // topoRank (extra `.map`) and starts FIRST, proving topoRank wins over start
    // order. Holds under both flags, since distinct ranks need no tie-break.
    for (fixEnabled <- Seq(true, false)) withFix144(fixEnabled) {
      withClue(s"[V18_TRX_ONSTART_FIX_144 = $fixEnabled] ") {
        val effects = mutable.Buffer[Effect[Int]]()
        val liveOwner = new TestableOwner // keeps the vars' signals alive
        val owner = new TestableOwner

        val var1 = Var(0)
        val var2 = Var(0)

        val updates1 = var1.signal.updates // lower topoRank
        val updates2 = var2.signal.map(identity).map(identity).updates // higher topoRank

        // Keep the source signals permanently started, so they retain their
        // state, then start & stop the `updates` streams so they'll need to
        // re-sync.
        var1.signal.foreach(_ => ())(liveOwner)
        var2.signal.foreach(_ => ())(liveOwner)

        val startStopOwner = new TestableOwner
        updates1.foreach(_ => ())(startStopOwner)
        updates2.foreach(_ => ())(startStopOwner)
        startStopOwner.killSubscriptions() // stop both `updates` streams

        // Update both vars while their `updates` streams are stopped, so each
        // `updates` stream has a missed value to re-emit when restarted.
        var1.set(1)
        var2.set(2)

        assert(updates1.debugTopoRank < updates2.debugTopoRank)

        val mergeStream = updates1.mergeWith(updates2)

        Transaction.onStart.shared {
          // Start updates2 (the HIGHER-rank stream) FIRST, to prove that start
          // order does NOT drive the emit order here - topoRank does.
          updates2.foreach(_ => ())(owner)
          mergeStream.foreach(v => effects += Effect("merge", v))(owner)
        }

        // Emits in topoRank order: updates1 (1, lower rank) then updates2 (2),
        // even though updates2 was started first.
        effects.toList shouldBe List(
          Effect("merge", 1),
          Effect("merge", 2)
        )
      }
    }
  }

  it("equal-rank signal.updates tie-break by merge argument index (only when fixed)") {
    // Equal-rank parents (the common case for two independent sources) used to
    // tie-break by arrival/insertion order = start order. With the parent-index
    // tie-break in MergeStream (part 2 of the fix), they now tie-break by merge
    // argument order. This isolates part 2 of the fix from part 1, because
    // signal.updates batches into the shared transaction regardless of the flag.
    for (fixEnabled <- Seq(true, false)) withFix144(fixEnabled) {
      withClue(s"[V18_TRX_ONSTART_FIX_144 = $fixEnabled] ") {
        val effects = mutable.Buffer[Effect[Int]]()
        val liveOwner = new TestableOwner
        val owner = new TestableOwner

        val var1 = Var(0)
        val var2 = Var(0)

        val updates1 = var1.signal.updates
        val updates2 = var2.signal.updates // same topoRank as updates1

        var1.signal.foreach(_ => ())(liveOwner)
        var2.signal.foreach(_ => ())(liveOwner)

        val startStopOwner = new TestableOwner
        updates1.foreach(_ => ())(startStopOwner)
        updates2.foreach(_ => ())(startStopOwner)
        startStopOwner.killSubscriptions()

        var1.set(1)
        var2.set(2)

        assert(updates1.debugTopoRank == updates2.debugTopoRank)

        val mergeStream = updates1.mergeWith(updates2)

        Transaction.onStart.shared {
          updates2.foreach(_ => ())(owner) // start updates2 first
          mergeStream.foreach(v => effects += Effect("merge", v))(owner)
        }

        val expected =
          if (fixEnabled) {
            // Tie broken by merge argument index: updates1 is arg 0, so `1`
            // comes first, even though updates2 was started first.
            List(Effect("merge", 1), Effect("merge", 2))
          } else {
            // Tie broken by insertion (= start) order: updates2 started first.
            List(Effect("merge", 2), Effect("merge", 1))
          }

        effects.toList shouldBe expected
      }
    }
  }

  // --- combine is NOT affected; sampleCombine's START-time sample IS ---------
  //
  // `combine` emits one combined value from the LATEST of each parent, only once
  // all parents have a value, so parent firing order can't reorder anything.
  //
  // `sampleCombine` (withCurrentValueOf) emits only when the SAMPLING stream
  // fires, reading the sampled signal's value at that moment. Pre-fix, if the
  // sampled's initial event is still in a later transaction, the sample is stale;
  // the fix puts both in one transaction where topoRank updates the sampled
  // first. So the fix improves sampleCombine's startup too - see below.

  it("combineStream(fromValue, fromValue) is start-order-independent") {
    for { fixEnabled <- Seq(true, false); preStartSecond <- List(false, true) } {
      withFix144(fixEnabled) {
        withClue(s"[fix=$fixEnabled, preStartSecond=$preStartSecond] ") {
          val effects = mutable.Buffer[(Int, Int)]()
          val owner = new TestableOwner
          val s1 = EventStream.fromValue(1)
          val s2 = EventStream.fromValue(2)

          Transaction.onStart.shared {
            if (preStartSecond) {
              s2.foreach(_ => ())(owner)
            }
            s1.combineWith(s2).foreach(effects += _)(owner)
          }

          effects.toList shouldBe List((1, 2))
        }
      }
    }
  }

  it("combineSignal(sig1, sig2) is start-order-independent") {
    for { fixEnabled <- Seq(true, false); preStartSecond <- List(false, true) } {
      withFix144(fixEnabled) {
        withClue(s"[fix=$fixEnabled, preStartSecond=$preStartSecond] ") {
          val effects = mutable.Buffer[(Int, Int)]()
          val owner = new TestableOwner
          val v1 = Var(1)
          val v2 = Var(2)

          Transaction.onStart.shared {
            if (preStartSecond) {
              v2.signal.foreach(_ => ())(owner)
            }
            v1.signal.combineWith(v2.signal).foreach(effects += _)(owner)
          }

          effects.toList shouldBe List((1, 2))
        }
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
