package com.raquo.airstream.ownership

/** All this class does is expose the method to kill subscriptions.
  * Just a small convenience for ad-hoc integrations
  *
  * Please note that this is NOT a [[OneTimeOwner]].
  * If that's what you need, copy this code but extend [[OneTimeOwner]] instead of [[Owner]].
  */
class ManualOwner extends Owner {

  override def killSubscriptions(): Unit = {
    super.killSubscriptions()
  }
}
