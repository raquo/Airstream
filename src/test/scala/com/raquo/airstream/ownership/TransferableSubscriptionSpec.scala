package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec

class TransferableSubscriptionSpec extends UnitSpec {

  private def makeDynamicOwner(): DynamicOwner = {
    new DynamicOwner(() => fail("Attempted to use permakilled owner!"))
  }

  it("none -> p1.inactive -> p1.activate -> p1.deactivate -> none") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()

    // --

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    sub.setOwner(parentOwner1) // inactive

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    parentOwner1.activate()

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    parentOwner1.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)

    deactivationCounter = 0

    // --

    sub.clearOwner()

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
  }

  it("none -> p1.active -> p1.deactivate -> p1.activate -> none") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()

    parentOwner1.activate()

    // --

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    sub.setOwner(parentOwner1)

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    parentOwner1.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)

    deactivationCounter = 0

    // --

    parentOwner1.activate()

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    sub.clearOwner()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)

    deactivationCounter = 0
  }

  it("none -> p1.active -> p2.inactive -> p3.active -> p3.deactivate -> none") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()
    val parentOwner3 = makeDynamicOwner()

    parentOwner1.activate()
    parentOwner3.activate()

    // --

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    sub.setOwner(parentOwner1)

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    sub.setOwner(parentOwner2)

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)

    deactivationCounter = 0

    // --

    sub.setOwner(parentOwner3)

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    parentOwner3.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)

    deactivationCounter = 0

    // --

    sub.clearOwner()

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
  }

  it("none -> p1.active -> p2.active -> p2.deactivate -> p3.inactive -> none") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()
    val parentOwner3 = makeDynamicOwner()

    parentOwner1.activate()
    parentOwner2.activate()

    // --

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    sub.setOwner(parentOwner1)

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    sub.setOwner(parentOwner2)

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)

    deactivationCounter = 0

    // --

    sub.setOwner(parentOwner3)

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    sub.clearOwner()

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
  }

  // -- Transfers made while the current owner is activating or deactivating --
  //  The owner reports being active throughout, but it reaches its subscriptions one by one.

  it("transfer out of an activating owner, before it reaches this subscription, activates it") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()

    parentOwner2.activate()

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    var isActiveAtTransfer: Option[Boolean] = None
    var isOwnerActiveAtTransfer: Option[Boolean] = None

    // Registered before `sub`, so it runs first when parentOwner1 activates
    DynamicSubscription.subscribeCallback(parentOwner1, _ => {
      isActiveAtTransfer = Some(sub.isActive)
      isOwnerActiveAtTransfer = Some(sub.isCurrentOwnerActive)
      sub.setOwner(parentOwner2)
    })

    sub.setOwner(parentOwner1) // inactive

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
    assert(!sub.isActive)

    // --

    parentOwner1.activate()

    assert(isActiveAtTransfer.contains(false)) // not reached yet...
    assert(isOwnerActiveAtTransfer.contains(true)) // ...but its owner already reports active

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)
    assert(sub.isActive)

    activationCounter = 0

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)
    assert(!sub.isActive)

    deactivationCounter = 0

    // --

    sub.clearOwner()

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
    assert(!sub.isActive)
  }

  it("transfer out of an activating owner, after it reached this subscription, is live") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()

    parentOwner2.activate()

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    var isActiveAtTransfer: Option[Boolean] = None
    var isOwnerActiveAtTransfer: Option[Boolean] = None

    sub.setOwner(parentOwner1) // inactive

    // Registered after `sub`, so it runs after `sub` is activated
    DynamicSubscription.subscribeCallback(parentOwner1, _ => {
      isActiveAtTransfer = Some(sub.isActive)
      isOwnerActiveAtTransfer = Some(sub.isCurrentOwnerActive)
      sub.setOwner(parentOwner2)
    })

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
    assert(!sub.isActive)

    // --

    parentOwner1.activate()

    assert(isActiveAtTransfer.contains(true))
    assert(isOwnerActiveAtTransfer.contains(true))

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)
    assert(sub.isActive)

    activationCounter = 0

    // --

    parentOwner1.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
    assert(sub.isActive)

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)
    assert(!sub.isActive)

    deactivationCounter = 0
  }

  it("transfer out of a deactivating owner, after it deactivated this subscription, activates it") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()

    parentOwner2.activate()

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    var isActiveAtTransfer: Option[Boolean] = None
    var isOwnerActiveAtTransfer: Option[Boolean] = None

    sub.setOwner(parentOwner1) // inactive

    // Registered after `sub`, so its cleanup runs after `sub` is deactivated
    DynamicSubscription.unsafe(parentOwner1, owner => new Subscription(owner, cleanup = () => {
      isActiveAtTransfer = Some(sub.isActive)
      isOwnerActiveAtTransfer = Some(sub.isCurrentOwnerActive)
      sub.setOwner(parentOwner2)
    }))

    parentOwner1.activate()

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)
    assert(sub.isActive)

    activationCounter = 0

    // --

    parentOwner1.deactivate()

    assert(isActiveAtTransfer.contains(false)) // already deactivated...
    assert(isOwnerActiveAtTransfer.contains(true)) // ...but its owner still reports active

    assert(activationCounter == 1)
    assert(deactivationCounter == 1)
    assert(sub.isActive)

    activationCounter = 0
    deactivationCounter = 0

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)
    assert(!sub.isActive)

    deactivationCounter = 0

    // --

    sub.clearOwner()

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
    assert(!sub.isActive)
  }

  it("transfer out of a deactivating owner, before it reaches this subscription, is live") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()

    parentOwner2.activate()

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    var isActiveAtTransfer: Option[Boolean] = None
    var isOwnerActiveAtTransfer: Option[Boolean] = None

    // Registered before `sub`, so its cleanup runs before `sub` is deactivated
    DynamicSubscription.unsafe(parentOwner1, owner => new Subscription(owner, cleanup = () => {
      isActiveAtTransfer = Some(sub.isActive)
      isOwnerActiveAtTransfer = Some(sub.isCurrentOwnerActive)
      sub.setOwner(parentOwner2)
    }))

    sub.setOwner(parentOwner1) // inactive

    parentOwner1.activate()

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)
    assert(sub.isActive)

    activationCounter = 0

    // --

    parentOwner1.deactivate()

    assert(isActiveAtTransfer.contains(true))
    assert(isOwnerActiveAtTransfer.contains(true))

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)
    assert(sub.isActive)

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)
    assert(!sub.isActive)

    deactivationCounter = 0
  }
}
