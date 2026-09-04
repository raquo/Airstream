package com.raquo.airstream.ownership

import com.raquo.airstream.core.{Named, Transaction}
import com.raquo.airstream.util.{FeatureFlags, JsResilientIterator}
import com.raquo.ew.JsArray

import scala.annotation.nowarn

// #Warning[Fragile] See JsResilientIterator scaladoc, be careful with edits, test thoroughly.

/** DynamicOwner manages [[DynamicSubscription]]-s similarly to how Owner manages `Subscription`s,
  * except `DynamicSubscription` can be activated and deactivated repeatedly.
  *
  * @param onAccessAfterKilled
  *          Called if you attempt to use any Owner created by this DynamicOwner
  *          after that Owner was killed.
  *          It's intended to log and/or throw for reporting / debugging purposes.
  */
class DynamicOwner(onAccessAfterKilled: () => Unit)
extends Named {

  /** Note: This is enforced to be a sorted set outside of the type system. #performance
    * Note: This should remain private, we don't want to expose the ability to kill individual
    *       subscriptions to code that didn't create those subscriptions.
    *       We rely on that in TransferableSubscription for example.
    */
  private val subscriptions: JsResilientIterator[DynamicSubscription] = new JsResilientIterator

  /** Legacy variable used for pre-V18 deferred removals logic (behind feature flag) */
  private var isSafeToRemoveSubscription = true

  /** Legacy variable used for pre-V18 deferred removals logic (behind feature flag) */
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

  // --

  def activate(): Unit = {
    if (!isActive) {
      Transaction.onStart.shared {
        val newOwner = new OneTimeOwner(onAccessAfterKilled)
        _maybeCurrentOwner = Some(newOwner)
        if (FeatureFlags.V18_IMMEDIATE_DYNSUB_REMOVAL_FIX_145: @nowarn("msg=deprecated")) {
          // Note: this does NOT iterate over any subscriptions that are added during the iteration.
          // Such new subs are activated in `addSubscription` below (since `_maybeCurrentOwner` is already set above).
          subscriptions.forEachExisting { sub =>
            sub.onActivate(newOwner)
          }
        } else {
          // Pre-V18 behaviour: Defer all removals until after the activation pass.
          // With deferral, removals don't shift the list during iteration (the iterator's cursor never adjusts).
          // See `removeSubscription`.
          isSafeToRemoveSubscription = false

          subscriptions.forEachExisting { sub =>
            sub.onActivate(newOwner)
          }

          removePendingSubscriptionsNow()
          isSafeToRemoveSubscription = true
        }
      }
    } else {
      throw new Exception(s"Can not activate $this: it is already active")
    }
  }

  def deactivate(): Unit = {
    // println(s"    - deactivating $this (numSubs=${subscriptions.length})")
    if (isActive) {
      Transaction.onStart.shared {
        // println(s"> deactivate $this")

        // We need to first deactivate all dynamic subscriptions.
        // If we killed the current owner's subscriptions first instead,
        // dynamic subscriptions would not have been notified about this,
        // and would carry dead subscriptions inside of them.

        if (FeatureFlags.V18_IMMEDIATE_DYNSUB_REMOVAL_FIX_145: @nowarn("msg=deprecated")) {
          subscriptions.forEachExisting(_.onDeactivate())

          // After DynamicSubscription-s were removed from the DynamicOwner,
          // we can now kill any other subscriptions that the user might
          // have added to the current non-dynamic Owner.
          _maybeCurrentOwner.foreach(_._killSubscriptions())

        } else {
          isSafeToRemoveSubscription = false

          subscriptions.forEachExisting(_.onDeactivate())

          removePendingSubscriptionsNow()

          _maybeCurrentOwner.foreach(_._killSubscriptions())

          removePendingSubscriptionsNow()

          isSafeToRemoveSubscription = true
        }

        _maybeCurrentOwner = None
      }
    } else {
      throw new Exception(s"Can not deactivate $this: it is not active")
    }
  }

  /** @param prepend  - If true, dynamic owner will prepend subscription to the list instead of appending.
    *                   This affects activation and deactivation order of subscriptions.
    */
  private[ownership] def addSubscription(subscription: DynamicSubscription, prepend: Boolean): Unit = {
    if (prepend) {
      subscriptions.prepend(subscription)
    } else {
      subscriptions.append(subscription)
    }
    _maybeCurrentOwner.foreach { o =>
      subscription.onActivate(o)
    }
  }

  private[ownership] def removeSubscription(subscription: DynamicSubscription): Unit = {
    if (isSafeToRemoveSubscription) {
      removeSubscriptionNow(subscription)
    } else {
      // This branch only happens under legacy pre-V18 behaviour
      pendingSubscriptionRemovals.push(subscription)
    }
  }

  private def removeSubscriptionNow(subscription: DynamicSubscription): Unit = {
    // Note: If we're mid-activation, `JsResilientIterator.remove` adjusts its cursor so that removing
    // this sub neither skips a not-yet-activated sub, nor re-visits a shifted one.
    val removed = subscriptions.remove(subscription)
    if (removed) {
      if (isActive) {
        subscription.onDeactivate()
      }
    } else {
      throw new Exception("Can not remove DynamicSubscription from DynamicOwner: subscription not found. Did you already kill it?")
    }
  }

  /** Legacy method used for pre-V18 deferred removals logic (behind feature flag) */
  private def removePendingSubscriptionsNow(): Unit = {
    // println("> removePendingSubscriptionsNow")
    // #TODO[Performance] Can we do a for-loop and then clear the whole array at once? Would that be 100% equivalent?
    while (pendingSubscriptionRemovals.length > 0) {
      val subscriptionToRemove = pendingSubscriptionRemovals.shift()
      removeSubscriptionNow(subscriptionToRemove)
    }
  }
}
