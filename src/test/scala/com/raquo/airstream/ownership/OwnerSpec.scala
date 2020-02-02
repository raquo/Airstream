package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.fixtures.{TestableOwner, TestableSubscription}

class OwnerSpec extends UnitSpec {

  it("Owner kills all possessions when discarded and continues to function") {

    val owner = new TestableOwner

    val ts1 = new TestableSubscription(owner)
    val ts2 = new TestableSubscription(owner)

    owner._testSubscriptions.toSeq shouldEqual List(ts1.subscription, ts2.subscription)
    ts1.killCount shouldBe 0
    ts2.killCount shouldBe 0

    owner.killSubscriptions()

    // Killing possessions calls kill on each of them exactly once, and then clears the list of possessions
    owner._testSubscriptions.toSeq shouldEqual Nil
    ts1.killCount shouldBe 1
    ts2.killCount shouldBe 1

    val ts3 = new TestableSubscription(owner)

    // Owner still functions as normal even after the killing spree
    owner._testSubscriptions.toSeq shouldEqual List(ts3.subscription)
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
}
