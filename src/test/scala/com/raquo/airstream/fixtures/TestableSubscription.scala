package com.raquo.airstream.fixtures

import com.raquo.airstream.ownership.{Owner, Subscription}

class TestableSubscription(owner: Owner) {

  var killCount = 0

  val subscription = new Subscription(owner, cleanup = () => {
    killCount += 1
  })
}
