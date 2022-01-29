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
}
