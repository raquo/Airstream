package com.raquo.airstream.ownership

import com.raquo.airstream.core.Named
import com.raquo.airstream.util.{FeatureFlags, JsResilientIterator}

import scala.annotation.{nowarn, unused}

/** Owner decides when to kill its subscriptions.
  *  - Ownership is defined at creation of the [[Subscription]]
  *  - Ownership is non-transferable
  *  - There is no way to unkill a Subscription
  *  - In other words: Owner can only own a Subscription once,
  *    and a Subscription can only ever be owned by its initial owner
  *  - Owner can still be used after calling killPossessions, but the canonical
  *    use case is for the Owner to kill its possessions when the owner itself
  *    is discarded (e.g. a UI component is unmounted).
  *
  * If you need something more flexible, use [[DynamicOwner]],
  * or build your own custom logic on top of this in a similar manner.
  */
trait Owner
extends Named {

  protected[this] val subscriptions: JsResilientIterator[Subscription] =
    new JsResilientIterator

  protected[this] def killSubscriptions(): Unit = {
    if (FeatureFlags.V18_IMMEDIATE_DYNSUB_REMOVAL_FIX_145: @nowarn("msg=deprecated")) {
      // We use `forEachExistingAndAppended`, not `forEachExisting`: a subscription's
      // user-defined `cleanup` function may potentially register a new subscription on
      // this same owner, which would append it to `subscriptions` mid-iteratino.
      // We must kill that new appended subscription too, otherwise the following `clear()`
      // would silently drop it without running the sub's `cleanup()` function – a leak.
      subscriptions.forEachExistingAndAppended { subscription =>
        if (!subscription.isKilled) {
          subscription.onKilledByOwner()
        }
      }
    } else {
      // Pre-V18 behaviour
      subscriptions.forEachSnapshotLegacy { subscription =>
        subscription.onKilledByOwner()
      }
    }
    subscriptions.clear()
  }

  // @TODO[API] This method only exists because I can't figure out how to better deal with permissions.
  @inline private[ownership] def _killSubscriptions(): Unit = killSubscriptions()

  /** This method will be called when this [[Owner]] has just started owning this resource.
    * You can override it to add custom behaviour.
    * Note: You can rely on this base method being empty.
    */
  protected[this] def onOwned(@unused subscription: Subscription): Unit = ()

  private[ownership] def onKilledExternally(subscription: Subscription): Unit = {
    val removed = subscriptions.remove(subscription)
    if (!removed) {
      throw new Exception("Can not remove Subscription from Owner: subscription not found.")
    }
  }

  private[ownership] def own(subscription: Subscription): Unit = {
    subscriptions.append(subscription)
    onOwned(subscription)
  }
}
