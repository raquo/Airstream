package com.raquo.airstream.ownership

import com.raquo.airstream.core.{Observable, Observer}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.DynamicOwner.PrivateOwner

import scala.scalajs.js

/** DynamicOwner manages [[DynamicSubscription]]-s similarly to how Owner manages `Subscription`s,
  * except `DynamicSubscription` can be activated and deactivated repeatedly.
  */
class DynamicOwner {

  /** Note: this is enforced to be a sorted set outside of the type system. #performance */
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

  def addObserver[A](
    observable: Observable[A],
    observer: Observer[A]
  ): DynamicSubscription = {
    subscribe(owner => observable.addObserver(observer)(owner))
  }

  def addSource[A](
    sourceStream: EventStream[A],
    targetBus: WriteBus[A]
  ): DynamicSubscription = {
    subscribe(owner => targetBus.addSource(sourceStream)(owner))
  }

  // @TODO[API] This method is experimental. Not sure if good idea. Think about memory management and stuff
  /** Add a dependent dynamic owner as a subscription.
    * It will be activated and deactivated at the same time as this dynamic owner */
//  def addChildDynamicOwner(
//    childOwner: DynamicOwner
//  ): DynamicSubscription = {
//    new DynamicSubscription(this, owner => {
//      childOwner.activate()
//      new Subscription(
//        owner,
//        cleanup = () => childOwner.deactivate()
//      )
//    })
//  }

  @inline def subscribe(activate: Owner => Subscription): DynamicSubscription = {
    new DynamicSubscription(this, activate)
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
