package com.raquo.airstream.state

import com.raquo.airstream.ownership.Subscription

trait OwnedSignal[+A] extends StrictSignal[A] {

  protected[this] val subscription: Subscription

  /** This only kills the subscription, but this signal might also have other listeners */
  def killOriginalSubscription(): Unit = subscription.kill()
} 
