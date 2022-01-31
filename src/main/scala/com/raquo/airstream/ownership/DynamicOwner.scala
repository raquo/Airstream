package com.raquo.airstream.ownership

import com.raquo.ew.JsArray

// @Warning[Fragile]
//  - We track a list of subscriptions and when activating / deactivating we run user code on each subscription
//  - This is potentially dangerous because said user code could add / remove more subscriptions from this DynamicOwner
//  - I think I've addressed those issues with `pendingSubscriptionRemovals`, but need to be very careful when changing anything here.
//  - Small things like `foreach` caching `subscriptions.length` are very important.

/** DynamicOwner manages [[DynamicSubscription]]-s similarly to how Owner manages `Subscription`s,
  * except `DynamicSubscription` can be activated and deactivated repeatedly.
  *
  * @param onAccessAfterKilled
  *          Called if you attempt to use any Owner created by this DynamicOwner
  *          after that Owner was killed.
  *          It's intended to log and/or throw for reporting / debugging purposes.
  */
class DynamicOwner(onAccessAfterKilled: () => Unit) {

  /** Note: This is enforced to be a sorted set outside of the type system. #performance
    * Note: This should remain private, we don't want to expose the ability to kill individual
    *       subscriptions to code that didn't create those subscriptions.
    *       We rely on that in TransferableSubscription for example.
    */
  private[this] val subscriptions: JsArray[DynamicSubscription] = JsArray()

  private var isSafeToRemoveSubscription = true

  private val pendingSubscriptionRemovals: JsArray[DynamicSubscription] = JsArray()

  private var _maybeCurrentOwner: Option[Owner] = None

  @inline def maybeCurrentOwner: Option[Owner] = _maybeCurrentOwner

  @inline def isActive: Boolean = _maybeCurrentOwner.isDefined

  @inline def hasSubscriptions: Boolean = numSubscriptions != 0

  /** Exposing this for testing mostly. Outside world should not need to know anything about this owner's subscriptions. */
  @inline def numSubscriptions: Int = subscriptions.length

  // @Note[API] We don't allow activating the DynamicOwner *while* it is being deactivated (and vice versa)
  //  - Currently this is enforced with isActive checks combined with the timing of updating `_maybeCurrentOwner`
  //    inside activate / deactivate methods.
  //  - That's a bit fragile, keep in mind

  private[this] var numPrependedSubs = 0

  def activate(): Unit = {
    if (!isActive) {
      val newOwner = new OneTimeOwner(onAccessAfterKilled)
      // @Note If activating a subscription adds another subscription, we must make sure to call onActivate on it.
      //  - the loop below does not do this because it fetches array length only once, at the beginning.
      //  - it is instead done by addSubscription by virtue of _maybeCurrentOwner being already defined at this point.
      //  - this is rather fragile, so maybe we should use a different foreach implementation.
      _maybeCurrentOwner = Some(newOwner)
      isSafeToRemoveSubscription = false
      numPrependedSubs = 0
      var i = 0;
      val originalNumSubs = subscriptions.length // avoid double-starting subs added during the loop. See the big comment above
      //println("    - start iteration of " + this)
      while (i < originalNumSubs) {
        // Prepending a sub while iterating shifts array indices, so we account for that
        //  - Use case for this: in Laminar controlled inputs logic, we create a prepend sub
        //    inside another dynamic subscription's activate callback
        val ix = i + numPrependedSubs
        val sub = subscriptions(ix)
        //println(s"    - activating ${sub} from iteration (ix = ${ix}, i = ${i}")
        sub.onActivate(newOwner)
        i += 1
      }
      //println(s"    - stop iteration of $this. numPrependedSubs = $numPrependedSubs")
      removePendingSubscriptionsNow()
      isSafeToRemoveSubscription = true
      numPrependedSubs = 0
    } else {
      throw new Exception(s"Can not activate $this: it is already active")
    }
  }

  def deactivate(): Unit = {
    //println(s"    - deactivating $this (numSubs=${subscriptions.length})")
    if (isActive) {
      // We need to first deactivate all dynamic subscriptions.
      // If we killed the current owner's subscriptions first instead,
      // dynamic subscriptions would not have been notified about this,
      // and would carry dead subscriptions inside of them.

      isSafeToRemoveSubscription = false

      subscriptions.forEach(_.onDeactivate())

      removePendingSubscriptionsNow()

      // After dynamic subscriptions were removed from the owner,
      // we can now kill any other subscriptions that the user might
      // have added to the current owner.
      _maybeCurrentOwner.foreach(_._killSubscriptions())

      removePendingSubscriptionsNow()

      isSafeToRemoveSubscription = true

      _maybeCurrentOwner = None
    } else {
      throw new Exception("Can not deactivate DynamicOwner: it is not active")
    }
  }

  /** @param prepend  - If true, dynamic owner will prepend subscription to the list instead of appending.
    *                   This affects activation and deactivation order of subscriptions.
    */
  private[ownership] def addSubscription(subscription: DynamicSubscription, prepend: Boolean): Unit = {
    if (prepend) {
      numPrependedSubs += 1
      subscriptions.unshift(subscription)
    } else {
      subscriptions.push(subscription)
    }
    _maybeCurrentOwner.foreach { o =>
      //println(s"    - activating ${subscription} after adding it to $this")
      subscription.onActivate(o)
    }
  }

  private[ownership] def removeSubscription(subscription: DynamicSubscription): Unit = {
    if (isSafeToRemoveSubscription) {
      removeSubscriptionNow(subscription)
    } else {
      pendingSubscriptionRemovals.push(subscription)
    }
  }

  private[this] def removeSubscriptionNow(subscription: DynamicSubscription): Unit = {
    val index = subscriptions.indexOf(subscription)
    if (index != -1) {
      subscriptions.splice(index, deleteCount = 1)
      if (isActive) {
        subscription.onDeactivate()
      }
    } else {
      throw new Exception("Can not remove DynamicSubscription from DynamicOwner: subscription not found. Did you already kill it?")
    }
  }

  private[this] def removePendingSubscriptionsNow(): Unit = {
    while (pendingSubscriptionRemovals.length > 0) {
      val subscriptionToRemove = pendingSubscriptionRemovals.shift()
      removeSubscriptionNow(subscriptionToRemove)
    }
  }
}
