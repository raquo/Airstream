package com.raquo.airstream.ownership

import com.raquo.airstream.ownership.DynamicOwner.PrivateOwner

import scala.scalajs.js

/** DynamicOwner manages [[DynamicSubscription]]-s similarly to how Owner manages `Subscription`s,
  * except `DynamicSubscription` can be activated and deactivated repeatedly.
  */
class DynamicOwner {

  /** Note: This is enforced to be a sorted set outside of the type system. #performance
    * Note: This should remain private, we don't want to expose the ability to kill individual
    *       subscriptions to code that didn't create those subscriptions.
    *       We rely on that in TransferableSubscription for example.
    */
  private[this] val subscriptions: js.Array[DynamicSubscription] = js.Array()

  private var _maybeCurrentOwner: Option[Owner] = None

  @inline def maybeCurrentOwner: Option[Owner] = _maybeCurrentOwner

  @inline def isActive: Boolean = maybeCurrentOwner.isDefined

  @inline def hasSubscriptions: Boolean = subscriptions.nonEmpty

  def activate(): Unit = {
    if (!isActive) {
      val newOwner = new PrivateOwner
      _maybeCurrentOwner = Some(newOwner)
      subscriptions.foreach(_.onActivate(newOwner))
    } else {
      throw new Exception("Can not activate DynamicOwner: it is already active")
    }
  }

  def deactivate(): Unit = {
    maybeCurrentOwner.fold {
      throw new Exception("Can not deactivate DynamicOwner: it is not active")
    } { currentOwner =>
      currentOwner._killSubscriptions()
      _maybeCurrentOwner = None
    }
  }

  private[ownership] def addSubscription(subscription: DynamicSubscription): Unit = {
    subscriptions.push(subscription)
    maybeCurrentOwner.foreach(subscription.onActivate)
  }

  private[ownership] def removeSubscription(subscription: DynamicSubscription): Unit = {
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
}

object DynamicOwner {

  /** This owner has no special logic, it is managed by the containing DynamicOwner */
  private class PrivateOwner extends Owner {}
}
