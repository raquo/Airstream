package com.raquo.airstream.ownership

// @TODO[API] Change cleanup from () => Unit to : => Unit? Would it be possible to override such a field?
/** Represents a leaky resource that needs to be cleaned up.
  *
  * Subscription is linked for its life to a given owner.
  * It can be killed by that owner, or externally using .kill().
  *
  * See also: Ownership documentation, as well as [[Owner]] scaladoc
  *
  * @param cleanup Note: Must not throw!
  */
class Subscription (
  private[ownership] val owner: Owner,
  cleanup: () => Unit
) {

  /** Make sure we only kill any given Subscription once. Just a sanity check against bad user logic,
    * e.g. calling .kill() manually when `owner` has already killed this subscription.
    */
  private[this] final var _isKilled = false

  owner.own(this)

  def isKilled: Boolean = _isKilled

  def kill(): Unit = {
    safeCleanup()
    owner.onKilledExternally(this)
  }

  // @TODO[API] This method exists only due to permissions. Is there another way?
  @inline private[ownership] def onKilledByOwner(): Unit = {
    safeCleanup()
  }

  private[this] def safeCleanup(): Unit = {
    if (!_isKilled) {
      cleanup()
      _isKilled = true
    } else {
      throw new Exception("Can not kill Subscription: it was already killed.")
    }
  }
}
