package com.raquo.airstream.ownership

import com.raquo.ew.JsArray

import scala.annotation.unused

/** Owner decides when to kill its subscriptions.
  * - Ownership is defined at creation of the [[Subscription]]
  * - Ownership is non-transferable
  * - There is no way to unkill a Subscription
  * - In other words: Owner can only own a Subscription once,
  *   and a Subscription can only ever be owned by its initial owner
  * - Owner can still be used after calling killPossessions, but the canonical
  *   use case is for the Owner to kill its possessions when the owner itself
  *   is discarded (e.g. a UI component is unmounted).
  *
  * If you need something more flexible, use [[DynamicOwner]],
  * or build your own custom logic on top of this in a similar manner.
  */
trait Owner {

  /** Note: This is enforced to be a sorted set outside the type system. #performance */
  protected[this] val subscriptions: JsArray[Subscription] = JsArray()

  protected[this] def killSubscriptions(): Unit = {
    subscriptions.forEach(_.onKilledByOwner())
    subscriptions.length = 0
  }

  // @TODO[API] This method only exists because I can't figure out how to better deal with permissions.
  @inline private[ownership] def _killSubscriptions(): Unit = killSubscriptions()

  /** This method will be called when this [[Owner]] has just started owning this resource.
    * You can override it to add custom behaviour.
    * Note: You can rely on this base method being empty.
    */
  protected[this] def onOwned(@unused subscription: Subscription): Unit = ()

  private[ownership] def onKilledExternally(subscription: Subscription): Unit = {
    val index = subscriptions.indexOf(subscription)
    if (index != -1) {
      subscriptions.splice(index, deleteCount = 1)
    } else {
      throw new Exception("Can not remove Subscription from Owner: subscription not found.")
    }
  }

  private[ownership] def own(subscription: Subscription): Unit = {
    subscriptions.push(subscription)
    onOwned(subscription)
  }
}
