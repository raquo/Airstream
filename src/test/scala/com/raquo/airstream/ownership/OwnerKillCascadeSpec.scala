package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.fixtures.TestableOwner

import scala.collection.mutable
import scala.util.Try

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
}
