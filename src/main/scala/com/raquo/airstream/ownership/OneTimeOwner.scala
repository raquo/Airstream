package com.raquo.airstream.ownership

/** Owner that can not be used after it was killed. Used in Laminar via [[DynamicOwner]].
  *
  * @param onAccessAfterKilled
  *          Called if you attempt to use this owner after it was killed.
  *          It's intended to log and/or throw for reporting / debugging purposes.
  */
class OneTimeOwner(onAccessAfterKilled: () => Unit) extends Owner {

  private var _isKilledForever: Boolean = false

  @inline def isKilledForever: Boolean = _isKilledForever

  override private[ownership] def own(subscription: Subscription): Unit = {
    if (_isKilledForever) {
      subscription.onKilledByOwner()
      onAccessAfterKilled()
    } else {
      super.own(subscription)
    }
  }

  override protected def killSubscriptions(): Unit = {
    super.killSubscriptions()
    _isKilledForever = true
  }
}
