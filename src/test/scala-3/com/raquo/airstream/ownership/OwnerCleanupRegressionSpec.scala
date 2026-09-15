package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec

import scala.collection.mutable

final class OwnerCleanupRegressionSpec extends UnitSpec {

  it("cleanup can kill a later sibling without invalidating owner disposal") {
    val owner = ManualOwner()
    val cleaned = mutable.Buffer.empty[String]
    var sibling: Subscription = null

    val first = Subscription(owner, () => {
      cleaned += "first"
      sibling.kill()
    })
    sibling = Subscription(owner, () => { cleaned += "sibling" })
    val last = Subscription(owner, () => { cleaned += "last" })

    // No callback throws or kills an already-killed subscription.
    // Removing sibling must not make the owner's iterator run past the array.
    owner.killSubscriptions()

    assert(cleaned.toList == List("first", "sibling", "last"))
    assert(first.isKilled && sibling.isKilled && last.isKilled)

    // Disposal must leave the owner empty and reusable.
    owner.killSubscriptions()
    val next = Subscription(owner, () => { cleaned += "next" })
    owner.killSubscriptions()
    assert(next.isKilled)
    assert(cleaned.toList == List("first", "sibling", "last", "next"))
  }
}
