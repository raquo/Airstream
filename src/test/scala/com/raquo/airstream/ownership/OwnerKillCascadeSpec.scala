package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.fixtures.TestableOwner

import scala.collection.mutable
import scala.util.{Random, Try}

/** Non-dynamic analogue of the DynamicOwner issue #145.
  *
  * These tests exercise what happens when one Subscription's `cleanup` kills
  * another Subscription belonging to the same Owner, while the Owner is in the
  * middle of `killSubscriptions()` (i.e. iterating over its subscriptions).
  *
  * Two distinct behaviours are asserted:
  *  - Killing a still-LIVE sibling must work: every subscription is cleaned up
  *    exactly once, no exception, and the owner ends up empty.
  *  - Killing an ALREADY-DEAD subscription (one the owner already cleaned in this
  *    same cascade, or the one currently being cleaned) is user error, and is
  *    rejected by the "already killed" sanity check in Subscription.
  */
class OwnerKillCascadeSpec extends UnitSpec {

  private def makeSub(
    label: String,
    owner: Owner,
    effects: mutable.Buffer[String],
    onCleanup: () => Unit = () => ()
  ): Subscription = {
    new Subscription(owner, cleanup = () => {
      effects.append(label)
      onCleanup()
    })
  }

  it("baseline - no cross-kill during killSubscriptions") {

    val effects = mutable.Buffer[String]()
    val owner = new TestableOwner

    val sub1 = makeSub("sub1", owner, effects)
    val sub2 = makeSub("sub2", owner, effects)
    val sub3 = makeSub("sub3", owner, effects)
    val _ = (sub1, sub2, sub3)

    owner.killSubscriptions()

    effects.toList shouldBe List("sub1", "sub2", "sub3")
    owner._testSubscriptions shouldBe Nil
  }

  it("cleanup kills a LATER, still-live subscription") {

    // sub1's cleanup kills sub3 before the kill-loop reaches it. sub3 is still
    // live, so this is a legitimate kill and must succeed.

    val effects = mutable.Buffer[String]()
    val owner = new TestableOwner

    lazy val sub1: Subscription = makeSub("sub1", owner, effects, onCleanup = () => sub3.kill())
    lazy val sub2: Subscription = makeSub("sub2", owner, effects)
    lazy val sub3: Subscription = makeSub("sub3", owner, effects)
    val _ = (sub1, sub2, sub3)

    owner.killSubscriptions()

    // Each sub cleaned exactly once, no crash, owner emptied.
    // sub3 is cleaned inline (during sub1's cleanup), before sub2.
    effects.toList shouldBe List("sub1", "sub3", "sub2")
    owner._testSubscriptions shouldBe Nil
  }

  it("cleanup kills the IMMEDIATE-NEXT sibling, and the owner stays reusable (#159)") {

    // Regression scenario from https://github.com/raquo/Airstream/pull/159.
    // Unlike the test above (which kills the LAST sub), sub1 here kills the sub
    // right after the cursor. Removing the item immediately after `iterIx` is the
    // case most likely to make the iterator "run past the array" if the cursor
    // bookkeeping in JsResilientIterator.remove is wrong. It must not: every sub
    // is cleaned once, in order, and the owner must be empty and reusable after.

    val effects = mutable.Buffer[String]()
    val owner = new TestableOwner

    lazy val sub1: Subscription = makeSub("sub1", owner, effects, onCleanup = () => sub2.kill())
    lazy val sub2: Subscription = makeSub("sub2", owner, effects)
    lazy val sub3: Subscription = makeSub("sub3", owner, effects)
    val _ = (sub1, sub2, sub3)

    owner.killSubscriptions()

    effects.toList shouldBe List("sub1", "sub2", "sub3")
    sub1.isKilled shouldBe true
    sub2.isKilled shouldBe true
    sub3.isKilled shouldBe true
    owner._testSubscriptions shouldBe Nil

    // Disposal must leave the owner empty and reusable: a second kill is a no-op,
    // and a freshly-owned sub is still killed by the next disposal.
    owner.killSubscriptions()
    val sub4 = makeSub("sub4", owner, effects)
    owner.killSubscriptions()

    sub4.isKilled shouldBe true
    effects.toList shouldBe List("sub1", "sub2", "sub3", "sub4")
    owner._testSubscriptions shouldBe Nil
  }

  it("cleanup that kills an EARLIER, already-cleaned subscription is rejected as a double-kill") {

    // sub3's cleanup kills sub1, which the owner already cleaned earlier in this
    // same cascade. Killing an already-dead subscription is user error.

    val effects = mutable.Buffer[String]()
    val owner = new TestableOwner

    lazy val sub1: Subscription = makeSub("sub1", owner, effects)
    lazy val sub2: Subscription = makeSub("sub2", owner, effects)
    lazy val sub3: Subscription = makeSub("sub3", owner, effects, onCleanup = () => sub1.kill())
    val _ = (sub1, sub2, sub3)

    val result = Try(owner.killSubscriptions())

    result.isFailure shouldBe true
    result.failed.get.getMessage.contains("already killed") shouldBe true

    // All three had their cleanup run (sub3's threw only after appending).
    effects.toList shouldBe List("sub1", "sub2", "sub3")
    // The cleanup threw, so the loop aborted before the wholesale list-clear;
    // the subscriptions remain in the (now dead) owner. This is the documented
    // consequence of a cleanup that throws, not a supported outcome.
    owner._testSubscriptions shouldBe List(sub1, sub2, sub3)
  }

  it("cleanup that adds a NEW subscription to the same (dying) owner still kills it") {

    // sub1's cleanup creates sub4 on the same owner while killSubscriptions is
    // iterating. sub4 is appended beyond the current pass's snapshotted bound, so
    // that pass never visits it. killSubscriptions loops to catch such stragglers,
    // so sub4 is still killed (its cleanup runs) instead of being leaked by a lone
    // `clear()`.

    val effects = mutable.Buffer[String]()
    val owner = new TestableOwner

    lazy val sub4: Subscription = makeSub("sub4", owner, effects)

    lazy val sub1: Subscription = makeSub("sub1", owner, effects, onCleanup = () => {
      val _ = sub4 // force creation of sub4 (registers it on `owner`) during sub1's cleanup
    })
    lazy val sub2: Subscription = makeSub("sub2", owner, effects)
    val _ = (sub1, sub2)

    owner.killSubscriptions()

    // sub4 is killed on a subsequent pass (after sub1, sub2), not leaked.
    effects.toList shouldBe List("sub1", "sub2", "sub4")
    sub4.isKilled shouldBe true
    owner._testSubscriptions shouldBe Nil
  }

  it("cleanup that kills ITSELF is rejected as a double-kill") {

    // sub2's cleanup kills sub2, which is mid-cleanup (already marked killed).

    val effects = mutable.Buffer[String]()
    val owner = new TestableOwner

    lazy val sub1: Subscription = makeSub("sub1", owner, effects)
    lazy val sub2: Subscription = makeSub("sub2", owner, effects, onCleanup = () => sub2.kill())
    lazy val sub3: Subscription = makeSub("sub3", owner, effects)
    val _ = (sub1, sub2, sub3)

    val result = Try(owner.killSubscriptions())

    result.isFailure shouldBe true
    result.failed.get.getMessage.contains("already killed") shouldBe true

    // sub2 was cleaned exactly once (no infinite re-entry) before it threw.
    effects.count(_ == "sub2") shouldBe 1

    // Note: Because the cleanup threw, it aborted the kill-loop, so sub3 was
    // never reached and the wholesale list-clear never ran. This is the
    // documented consequence of a cleanup that throws ("cleanup Must not
    // throw!"), not a supported outcome.
    effects.toList shouldBe List("sub1", "sub2")
    owner._testSubscriptions shouldBe List(sub1, sub2, sub3)
  }

  // -- Randomized cascade stress test --------------------------------------
  //
  // The single-case tests above cover specific positions (kill LATER, kill
  // IMMEDIATE-NEXT, kill EARLIER, spawn NEW, kill SELF). This section fuzzes
  // arbitrary *legal* combinations of them so we don't miss a cursor edge case
  // that only shows up for some particular interleaving.
  //
  // Reproducibility: each case is driven entirely by `new Random(seed)` with a
  // fixed list of seeds and no wall-clock or ambient randomness, and each seed
  // is its own named `it(...)`. A failure therefore names the exact seed that
  // reproduces it — no undebuggable flukes.

  private val FuzzSeeds: Seq[Long] = 0L until 500L

  FuzzSeeds.foreach { seed =>
    it(s"cascade stays consistent under random legal mutations (seed $seed)") {
      runCascadeFuzz(seed)
    }
  }

  private def runCascadeFuzz(seed: Long): Unit = {
    val rng = new Random(seed)
    val owner = new TestableOwner

    // Every subscription ever created for this owner, in creation order.
    val allSubs = mutable.ArrayBuffer.empty[Subscription]
    // How many times each subscription's cleanup ran.
    val cleanupCounts = mutable.LinkedHashMap.empty[Subscription, Int]
    // Cap on subscriptions created during cleanup, so a "spawn" cascade terminates.
    var spawnBudget = 20

    def newSub(): Subscription = {
      // Forward reference so a subscription's own cleanup can inspect `self`.
      var self: Subscription = null
      val sub = new Subscription(owner, cleanup = () => {
        cleanupCounts(self) = cleanupCounts.getOrElse(self, 0) + 1

        // Maybe kill some still-live sibling (could be before or after the cursor).
        if (rng.nextInt(100) < 55) {
          val liveSiblings = allSubs.filter(s => (s ne self) && !s.isKilled)
          if (liveSiblings.nonEmpty) {
            liveSiblings(rng.nextInt(liveSiblings.size)).kill()
          }
        }

        // Maybe register a brand-new subscription on this same dying owner.
        if (spawnBudget > 0 && rng.nextInt(100) < 35) {
          spawnBudget -= 1
          val _ = newSub()
        }
      })
      self = sub
      cleanupCounts.getOrElseUpdate(sub, 0)
      allSubs += sub
      sub
    }

    val initialCount = 1 + rng.nextInt(8)
    (0 until initialCount).foreach(_ => newSub())

    // The whole point: this must not throw, no matter what the cleanups do.
    owner.killSubscriptions()

    // Every subscription ever created (initial + spawned) is cleaned exactly once.
    allSubs.foreach { sub =>
      withClue(s"seed=$seed sub=$sub cleanupCount=${cleanupCounts(sub)}: ") {
        sub.isKilled shouldBe true
        cleanupCounts(sub) shouldBe 1
      }
    }

    // The owner is left empty and reusable.
    owner._testSubscriptions shouldBe Nil

    val reusedSub = new Subscription(owner, cleanup = () => ())
    owner.killSubscriptions()
    reusedSub.isKilled shouldBe true
    owner._testSubscriptions shouldBe Nil
  }
}
