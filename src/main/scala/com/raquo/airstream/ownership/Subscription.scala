package com.raquo.airstream.ownership

// @TODO[API] Change cleanup from () => Unit to : => Unit? WOuld it be possible to override such a field?
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

  // @TODO[API] Perhaps we should let subclasses read this? If anyone cares, let me know.
  /** Make sure we only kill any given Subscription once. Just a sanity check against bad user logic,
    * e.g. calling .kill() manually when `owner` has already killed this subscription.
    */
  private[this] final var isKilled = false

  owner.own(this)

  def kill(): Unit = {
    safeCleanup()
    owner.onKilledExternally(this)
  }

  // @TODO[API] This method exists only due to permissions. Is there another way?
  @inline private[ownership] def onKilledByOwner(): Unit = {
    safeCleanup()
  }

  private[this] def safeCleanup(): Unit = {
    if (!isKilled) {
      cleanup()
      isKilled = true
    } else {
      throw new Exception("Can not kill Subscription: it was already killed.")
    }
  }
}
