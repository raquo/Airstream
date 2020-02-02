package com.raquo.airstream.fixtures

import com.raquo.airstream.ownership.{Owner, Subscription}

import scala.scalajs.js

class TestableOwner extends Owner {

  def _testSubscriptions: js.Array[Subscription] = subscriptions

  override def killSubscriptions(): Unit = {
    super.killSubscriptions()
  }
}
