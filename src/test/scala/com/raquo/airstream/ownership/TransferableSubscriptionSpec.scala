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

  // The tests below transfer `sub` from inside its owner's activation / deactivation pass.
  // The owner reports `isActive` for the whole pass, but `sub` only changes state when the
  // pass reaches it. In Laminar: an element moved, on mount, into a list that its parent
  // activates before the element itself (e.g. `div(div(children <-- ...), element)`).

  it("p1.activate: stolen into active p2 BEFORE p1 reached it -> activates in p2") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    // Registered before `sub`, so p1 runs it first
    DynamicSubscription.subscribeCallback(
      parentOwner1,
      _ => {
        parentOwner2.activate()
        sub.setOwner(parentOwner2)
      }
    )

    sub.setOwner(parentOwner1) // inactive

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    parentOwner1.activate()

    // Not a live transfer: `sub` was never activated in p1
    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    parentOwner1.deactivate() // `sub` no longer lives here

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)
  }

  it("p1.activate: stolen into active p2 AFTER p1 reached it -> live transfer") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    sub.setOwner(parentOwner1) // inactive

    // Registered after `sub`, so p1 activates `sub` first
    DynamicSubscription.subscribeCallback(
      parentOwner1,
      _ => {
        parentOwner2.activate()
        sub.setOwner(parentOwner2)
      }
    )

    assert(activationCounter == 0)
    assert(deactivationCounter == 0)

    // --

    parentOwner1.activate()

    // Activated once in p1, then transferred to p2 seamlessly
    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)
  }

  it("p1.deactivate: stolen into active p2 AFTER p1 deactivated it -> re-activates in p2") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = makeDynamicOwner()
    val parentOwner2 = makeDynamicOwner()

    parentOwner1.activate()
    parentOwner2.activate()

    val sub = new TransferableSubscription(() => activationCounter += 1, () => deactivationCounter += 1)

    sub.setOwner(parentOwner1)

    // Registered after `sub`, so p1 deactivates `sub` first
    DynamicSubscription.unsafe(
      parentOwner1,
      owner => new Subscription(owner, cleanup = () => sub.setOwner(parentOwner2))
    )

    assert(activationCounter == 1)
    assert(deactivationCounter == 0)

    activationCounter = 0

    // --

    parentOwner1.deactivate()

    // Not a live transfer: `sub` was already deactivated when p2 took it
    assert(activationCounter == 1)
    assert(deactivationCounter == 1)

    activationCounter = 0
    deactivationCounter = 0

    // --

    parentOwner2.deactivate()

    assert(activationCounter == 0)
    assert(deactivationCounter == 1)
  }
}
