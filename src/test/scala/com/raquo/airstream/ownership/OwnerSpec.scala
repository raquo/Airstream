package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.fixtures.{TestableOneTimeOwner, TestableOwner, TestableSubscription}

import scala.util.Try

class OwnerSpec extends UnitSpec {

  it("Owner kills all possessions when discarded and continues to function") {

    val owner = new TestableOwner

    val ts1 = new TestableSubscription(owner)
    val ts2 = new TestableSubscription(owner)

    owner._testSubscriptions shouldBe List(ts1.subscription, ts2.subscription)
    ts1.killCount shouldBe 0
    ts2.killCount shouldBe 0

    owner.killSubscriptions()

    // Killing possessions calls kill on each of them exactly once, and then clears the list of possessions
    owner._testSubscriptions shouldBe Nil
    ts1.killCount shouldBe 1
    ts2.killCount shouldBe 1

    val ts3 = new TestableSubscription(owner)

    // Owner still functions as normal even after the killing spree
    owner._testSubscriptions shouldBe List(ts3.subscription)
    ts1.killCount shouldBe 1
    ts2.killCount shouldBe 1
    ts3.killCount shouldBe 0

    owner.killSubscriptions()
    ts1.killCount shouldBe 1
    ts2.killCount shouldBe 1
    ts3.killCount shouldBe 1

    // Double-check that killing again does not result in double-kill
    owner.killSubscriptions()
    ts1.killCount shouldBe 1
    ts2.killCount shouldBe 1
    ts3.killCount shouldBe 1

    // @TODO[Airstream] Check that killing manually does not result in double-kill
  }

  it("OneTimeOwner is unusable after it's killed") {

    var errorCallbackCounter = 0
    var cleanedCounter = 0

    val owner = new TestableOneTimeOwner(() => {
      errorCallbackCounter += 1
    })

    val sub1 = new Subscription(owner, cleanup = () => cleanedCounter += 1)
    val sub2 = new Subscription(owner, cleanup = () => cleanedCounter += 1)

    owner._testSubscriptions shouldBe List(sub1, sub2)

    // --

    owner.killSubscriptions()

    owner._testSubscriptions shouldBe Nil
    errorCallbackCounter shouldBe 0
    cleanedCounter shouldBe 2

    cleanedCounter = 0

    // --

    owner.killSubscriptions()
    errorCallbackCounter shouldBe 0
    cleanedCounter shouldBe 0

    // --

    val sub3 = new Subscription(owner, cleanup = () => cleanedCounter += 1)

    owner._testSubscriptions shouldBe Nil
    errorCallbackCounter shouldBe 1
    cleanedCounter shouldBe 1

    errorCallbackCounter = 0
    cleanedCounter = 0

    Try(sub3.kill()).isFailure shouldBe true // Can not kill already killed subscription
  }

  it("OneTimeOwner handles callbacks that throw") {

    var cleanedCounter = 0

    val owner = new TestableOneTimeOwner(() => throw new Exception("OneTimeOwner misused!"))

    val sub1 = new Subscription(owner, cleanup = () => cleanedCounter += 1)
    val sub2 = new Subscription(owner, cleanup = () => cleanedCounter += 1)

    owner._testSubscriptions shouldBe List(sub1, sub2)

    // --

    owner.killSubscriptions()

    owner._testSubscriptions shouldBe Nil
    cleanedCounter shouldBe 2

    cleanedCounter = 0

    // --

    owner.killSubscriptions()
    cleanedCounter shouldBe 0

    // --

    Try(new Subscription(owner, cleanup = () => cleanedCounter += 1)).isFailure shouldBe true

    owner._testSubscriptions shouldBe Nil
    cleanedCounter shouldBe 1 // verify subscription was cleaned up

    cleanedCounter = 0
  }

  // #TODO I decided not to implement the changes in behaviour below for now.
  //  That would need us to call _isKilledForever = true FIRST in OneTimeOwner.killSubscriptions.
  //  It seems that this could have unintended consequences, not worth it / no time to figure it out.

  // it("OneTimeOwner - a subscription created during teardown is killed AND reported via onAccessAfterKilled") {
  //
  //   // A subscription's cleanup registers a NEW subscription on this same owner while
  //   // killSubscriptions is running. Because OneTimeOwner marks itself killed BEFORE
  //   // tearing down, that straggler is treated as "access after killed": its cleanup
  //   // runs (no leak) AND onAccessAfterKilled fires, consistent with a subscription
  //   // created after teardown finished. (Non-throwing hook here.)
  //
  //   var errorCallbackCounter = 0
  //   var cleanedCounter = 0
  //
  //   val owner = new TestableOneTimeOwner(() => {
  //     errorCallbackCounter += 1
  //   })
  //
  //   var maybeStraggler: Option[Subscription] = None
  //
  //   val sub1 = new Subscription(owner, cleanup = () => {
  //     cleanedCounter += 1
  //     maybeStraggler = Some(new Subscription(owner, cleanup = () => cleanedCounter += 1))
  //   })
  //   val sub2 = new Subscription(owner, cleanup = () => cleanedCounter += 1)
  //   val sub3 = new Subscription(owner, cleanup = () => cleanedCounter += 1)
  //
  //   owner._testSubscriptions shouldBe List(sub1, sub2, sub3)
  //
  //   // --
  //
  //   owner.killSubscriptions()
  //
  //   // sub1, straggler, sub2, sub3 all cleaned exactly once (straggler NOT leaked).
  //   cleanedCounter shouldBe 4
  //   // Only the straggler (created after the owner marked itself killed) triggered the hook.
  //   errorCallbackCounter shouldBe 1
  //   // The straggler was diverted in `own` and never tracked; the pass's subs are cleared.
  //   owner._testSubscriptions shouldBe Nil
  //   maybeStraggler.get.isKilled shouldBe true
  // }
  //
  // it("OneTimeOwner - a straggler created during teardown with a throwing hook aborts the rest of the teardown") {
  //
  //   // Same as above, but the hook throws (as it does in Laminar). The throw propagates
  //   // out of the teardown pass, so the remaining sibling is not reached and the list is
  //   // not cleared. This pins that consequence: it's the intended loud signal, same
  //   // category as a `cleanup` that throws.
  //
  //   var cleanedCounter = 0
  //
  //   val owner = new TestableOneTimeOwner(() => throw new Exception("OneTimeOwner misused!"))
  //
  //   val sub1 = new Subscription(owner, cleanup = () => {
  //     cleanedCounter += 1
  //     val _ = new Subscription(owner, cleanup = () => cleanedCounter += 1) // straggler
  //   })
  //   val sub2 = new Subscription(owner, cleanup = () => cleanedCounter += 1)
  //
  //   owner._testSubscriptions shouldBe List(sub1, sub2)
  //
  //   // --
  //
  //   val result = Try(owner.killSubscriptions())
  //
  //   result.isFailure shouldBe true
  //   result.failed.get.getMessage.contains("misused") shouldBe true
  //
  //   // sub1 and the straggler were cleaned (the straggler's cleanup runs before the hook
  //   // throws), but the throw aborted the pass before sub2, and before clear().
  //   cleanedCounter shouldBe 2
  //   owner._testSubscriptions shouldBe List(sub1, sub2)
  // }
}
