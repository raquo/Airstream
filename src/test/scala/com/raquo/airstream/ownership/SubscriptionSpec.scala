package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.fixtures.TestableOwner

import scala.util.Try

class SubscriptionSpec extends UnitSpec {

  it("a throwing cleanup stays killed and is not run again") {
    // Defensive coverage for misuse: cleanup must not throw under the documented contract.
    val owner = new TestableOwner
    val error = new Exception("cleanup failed")
    var cleanCount = 0
    val sub = new Subscription(owner, cleanup = () => {
      cleanCount += 1
      throw error
    })

    Try(sub.kill()).failed.get shouldBe error
    sub.isKilled shouldBe true
    Try(sub.kill()).isFailure shouldBe true
    cleanCount shouldBe 1
    owner.killSubscriptions()
    owner._testSubscriptions shouldBe Nil
    cleanCount shouldBe 1
  }

  it("re-entrant kill() inside cleanup does not re-run cleanup (safeCleanup guard)") {

    // Subscription whose cleanup calls its own kill().
    // The re-entrant kill must be rejected as "already killed" instead of causing infinite recursion.

    val owner = new TestableOwner

    var cleanCount = 0
    var reentrantResult: Try[Unit] = null

    lazy val sub: Subscription = new Subscription(owner, cleanup = () => {
      cleanCount += 1
      reentrantResult = Try(sub.kill()) // re-entrant self-kill during cleanup
    })
    val _ = sub // force initialization

    sub.kill()

    cleanCount shouldBe 1 // cleanup ran exactly once, no recursion

    reentrantResult.isFailure shouldBe true
    reentrantResult.failed.toOption.exists(_.getMessage.contains("already killed")) shouldBe true

    sub.isKilled shouldBe true
    owner._testSubscriptions shouldBe Nil
  }
}
