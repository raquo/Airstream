package com.raquo.airstream.ownership

/** Represents a subscription that can be turned on and off repeatedly.
  * For example, in Laminar the elements can be mounted and unmounted repeatedly,
  * and so their subscriptions are activated and deactivated respectively when
  * those events happen.
  *
  * In contrast, the only thing you can do to a non-dynamic [[Subscription]] is `kill` it,
  * and once that is done, it will remain dead forever.
  *
  * Note that the dynamic subscription is NOT activated automatically upon creation.
  *
  * @param activate Note: Must not throw!
  */
class DynamicSubscription (
  dynamicOwner: DynamicOwner,
  activate: Owner => Subscription
) {

  private[this] var maybeCurrentSubscription: Option[Subscription] = None

  dynamicOwner.addSubscription(this)

  @inline def isActive: Boolean = maybeCurrentSubscription.isDefined

  /** Permanently kill this subscription, deactivating if it's currently active, and removing it from the dynamic owner */
  def kill(): Unit = dynamicOwner.removeSubscription(this)

  private[ownership] def onActivate(owner: Owner): Unit = {
    maybeCurrentSubscription = Some(activate(owner))
  }

  private[ownership] def onDeactivate(): Unit = {
    maybeCurrentSubscription.fold {
      throw new Exception("Can not deactivate DynamicSubscription: it is not active")
    } { currentSubscription =>
      currentSubscription.kill()
      maybeCurrentSubscription = None
    }
  }
}

