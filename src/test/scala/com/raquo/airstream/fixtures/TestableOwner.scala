package com.raquo.airstream.fixtures

import com.raquo.airstream.ownership.{Owner, Subscription}

class TestableOwner extends Owner {

  def _testSubscriptions: List[Subscription] = subscriptions.asScalaJs.toList

  override def killSubscriptions(): Unit = {
    super.killSubscriptions()
  }
}
