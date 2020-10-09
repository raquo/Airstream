package com.raquo.airstream.fixtures

import com.raquo.airstream.ownership.{OneTimeOwner, Subscription}

class TestableOneTimeOwner(onAccessAfterKilled: () => Unit) extends OneTimeOwner(onAccessAfterKilled){

  def _testSubscriptions: List[Subscription] = subscriptions.toList

  override def killSubscriptions(): Unit = super.killSubscriptions()
}
