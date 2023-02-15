package com.raquo.airstream.ownership

import com.raquo.airstream.core.{EventStream, Observable, Observer, Sink, Transaction}
import com.raquo.airstream.eventbus.WriteBus

// @TODO[API] I could make the constructor public but it's less confusing if you use the companion object methods

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
  * The subscription created by `activate` must not be killed externally,
  * otherwise DynamicSubscription will throw when it tries to kill it and
  * it's already killed.
  *
  * @param activate - Note: Must not throw!
  * @param prepend  - If true, dynamic owner will prepend subscription to the list instead of appending.
  *                   This affects activation and deactivation order of subscriptions.
  */
class DynamicSubscription private (
  dynamicOwner: DynamicOwner,
  activate: Owner => Option[Subscription],
  prepend: Boolean
) {

  // @Note this can be None even if this dynamic subscription is active (if activate() returned None)
  private[this] var maybeCurrentSubscription: Option[Subscription] = None

  dynamicOwner.addSubscription(this, prepend)

  @inline def isOwnerActive: Boolean = dynamicOwner.isActive

  @inline def isOwnedBy(owner: DynamicOwner): Boolean = owner == dynamicOwner

  /** Permanently kill this subscription, deactivating if it's currently active, and removing it from the dynamic owner */
  def kill(): Unit = dynamicOwner.removeSubscription(this)

  private[ownership] def onActivate(owner: Owner): Unit = {
    //println(s"    - activating $this")
    // I don't think Laminar itself needs onStart.shared here, this is for users' custom dynamic subscriptions.
    // Honestly this might be overkill, but I think this is cheap, and diagnosing these kinds of bugs is expensive.
    Transaction.onStart.shared {
      maybeCurrentSubscription = activate(owner)
    }
  }

  private[ownership] def onDeactivate(): Unit = {
    maybeCurrentSubscription.foreach { currentSubscription =>
      currentSubscription.kill()
      maybeCurrentSubscription = None
    }
  }
}

object DynamicSubscription {

  /** Use this when your activate() code requires cleanup on deactivation.
    * Specify that cleanup code inside the resulting Subscription.
    *
    * Marked as "unsafe" because you must not kill() the subscription
    * created by `activate`, it must be managed by this DynamicSubscription
    * only.
    *
    * @param activate Note: Must not throw! Must not kill resulting subscription!
    */
  def unsafe(
    dynamicOwner: DynamicOwner,
    activate: Owner => Subscription,
    prepend: Boolean = false
  ): DynamicSubscription = {
    new DynamicSubscription(dynamicOwner, (owner: Owner) => Some(activate(owner)), prepend)
  }

  /** Use this when your activate() code does not require a cleanup on deactivation.
    *
    * @param activate Note: Must not throw!
    */
  def subscribeCallback(
    dynamicOwner: DynamicOwner,
    activate: Owner => Unit,
    prepend: Boolean = false
  ): DynamicSubscription = {
    new DynamicSubscription(dynamicOwner, (owner: Owner) => {
      activate(owner)
      None
    }, prepend)
  }

  @inline def subscribeObserver[A](
    dynamicOwner: DynamicOwner,
    observable: Observable[A],
    observer: Observer[A]
  ): DynamicSubscription = {
    subscribeSink(dynamicOwner, observable, observer)
  }

  def subscribeSink[A](
    dynamicOwner: DynamicOwner,
    observable: Observable[A],
    sink: Sink[A]
  ): DynamicSubscription = {
    DynamicSubscription.unsafe(dynamicOwner, owner => observable.addObserver(sink.toObserver)(owner))
  }

  def subscribeFn[A](
    dynamicOwner: DynamicOwner,
    observable: Observable[A],
    onNext: A => Unit
  ): DynamicSubscription = {
    DynamicSubscription.unsafe(dynamicOwner, owner => observable.foreach(onNext)(owner))
  }

  def subscribeBus[A](
    dynamicOwner: DynamicOwner,
    eventStream: EventStream[A],
    writeBus: WriteBus[A]
  ): DynamicSubscription = {
    DynamicSubscription.unsafe(dynamicOwner, owner => writeBus.addSource(eventStream)(owner))
  }
}
