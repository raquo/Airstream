package com.raquo.airstream.state

import com.raquo.airstream.core.WritableSignal
import com.raquo.airstream.ownership.Subscription

// @TODO[API] Should we expose `killOriginalSubscription` to end users?
trait OwnedSignal[+A] extends StrictSignal[A] {
  self: WritableSignal[A] =>

  protected[this] val subscription: Subscription

  /** This only kills the subscription, but this signal might also have other listeners */
  def killOriginalSubscription(): Unit = subscription.kill()
}
