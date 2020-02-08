package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec

class TransferableSubscriptionSpec extends UnitSpec {

  it("none -> p1.inactive -> p1.activate -> p1.deactivate -> none") {

    var activationCounter = 0
    var deactivationCounter = 0

    val parentOwner1 = new DynamicOwner

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

    val parentOwner1 = new DynamicOwner

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

    val parentOwner1 = new DynamicOwner
    val parentOwner2 = new DynamicOwner
    val parentOwner3 = new DynamicOwner

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

    val parentOwner1 = new DynamicOwner
    val parentOwner2 = new DynamicOwner
    val parentOwner3 = new DynamicOwner

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
}
