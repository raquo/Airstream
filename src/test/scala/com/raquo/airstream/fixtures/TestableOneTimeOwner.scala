package com.raquo.airstream.fixtures

import com.raquo.airstream.ownership.{OneTimeOwner, Subscription}

class TestableOneTimeOwner(onAccessAfterKilled: () => Unit) extends OneTimeOwner(onAccessAfterKilled){

  def _testSubscriptions: List[Subscription] = subscriptions.asScalaJs.toList

  override def killSubscriptions(): Unit = super.killSubscriptions()
}
