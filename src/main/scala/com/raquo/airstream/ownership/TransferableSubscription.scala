package com.raquo.airstream.ownership

/** This subscription is hyper dynamic, allowing you to change DynamicOwner on the fly.
  *
  * It works by creating DynamicSubscription-s under the hood with your provided owner and activate()
  * and deactivate() methods, but it has a special semantic: when transferring this subscription
  * from one active DynamicOwner to another active DynamicOwner, neither activate() nor deactivate()
  * are called because continuity of active ownership is maintained.
  *
  * So in effect, this subscription only cares whether it's owned or not, so it does not expose the
  * owner to you: notice the `activate` callback is not provided with an Owner.
  *
  * An example of where this is useful is tracking mounting and unmounting of elements in Laminar.
  * If an element is mounted, we want to call activate(), if unmounted, we want to call deactivate(),
  * but if the element is MOVED from one mounted parent to another mounted parent, it just remains
  * mounted, this transition is of no interest to us. If not for this subscription's special design,
  * we would need to call deactivate() to "detach" the subscription from its old parent's owner and
  * then immediately afterwards call activate() to "attach" the subscription to the new parent's owner,
  * but that would deactivate and then immediately re-activate all subscriptions on the Laminar element
  * being moved (and all of its descendants), which would be very wasteful. Well, you do need to know
  * Laminar to understand this example.
  */
class TransferableSubscription(
  activate: () => Unit,
  deactivate: () => Unit
) {

  /** This is None initially, and when the last subscription was .kill()-ed.
    * Simply deactivating a subscription does not remove it from there.
    */
  private var maybeSubscription: Option[DynamicSubscription] = None

  /** Whether we are currently transferring this subscription from one active owner to another active owner. */
  private var isLiveTransferInProgress: Boolean = false

  def hasOwner: Boolean = maybeSubscription.nonEmpty

  def isCurrentOwnerActive: Boolean = maybeSubscription.exists(_.isOwnerActive)

  /** Update the owner of this subscription. */
  def setOwner(nextOwner: DynamicOwner): Unit = {
    //println(s"    - setOwner of ${this} to ${nextOwner}")
    if (isLiveTransferInProgress) {
      throw new Exception("Unable to set owner on DynamicTransferableSubscription while a transfer on this subscription is already in progress.")
    }

    // @Note this short-circuit is important. As explained in Laminar comments,
    //  when activating / deactivating owners, we have to iterate through their
    //  subscriptions and run user code for each of them. That user code
    //  might trigger other activations / deactivations, and those would be
    //  processed without delay, in this half-activated state where some
    //  subscription could be active but its owner not active, or the other way.
    //  So to be safer, we just short circuit here instead of relying on complex
    //  logic doing what we need in a potentially inconsistent state.
    if (maybeSubscription.exists(_.isOwnedBy(nextOwner))) {
      // Owner is the same – do nothing
    } else {
      if (isCurrentOwnerActive && nextOwner.isActive) {
        isLiveTransferInProgress = true
      }

      // It's hard to wrap your head around this isLiveTransferInProgress logic.
      //  - essentially when transferring this subscription from one owner to another
      //    we don't want activate() or deactivate() calls to happen because the
      //    subscription still has an owner, we're just changing who it is.
      //  - to achieve this we use this internal isLiveTransferInProgress flag that
      //    we look up to see when we shouldn't make those activate() and deactivate()
      //    calls.

      // Remember that killing a subscription will only call deactivate() if we're not transferring
      maybeSubscription.foreach { subscription =>
        subscription.kill()
        maybeSubscription = None
      }

      val newPilotSubscription = DynamicSubscription.unsafe(
        nextOwner,
        activate = parentOwner => {
          // If transfer is in progress, this activate method will be called immediately
          // in this NEW subscription that we're creating, so we need to skip activation
          // because there is no gap in ownership, just a transfer.
          // (proof – the previous subscription is active)
          if (!isLiveTransferInProgress) {
            //println(s"    - activating pilot dynSub which is:")
            activate()
          }
          new Subscription(parentOwner, cleanup = () => {
            // If transfer is in progress when this cleanup happens, this means this cleanup
            // method was be called when killing the PREVIOUS subscription that we're replacing,
            // so we need to skip deactivation here because now the NEW subscription will do it instead.
            if (!isLiveTransferInProgress) {
              deactivate()
            }
          })
        }
      )

      //println(s"    - created pilot $newPilotSubscription")

      maybeSubscription = Some(newPilotSubscription)

      isLiveTransferInProgress = false
    }
  }

  def clearOwner(): Unit = {
    if (isLiveTransferInProgress) {
      throw new Exception("Unable to clear owner on DynamicTransferableSubscription while a transfer on this subscription is already in progress.")
    }

    maybeSubscription.foreach { subscription =>
      // @Warning[Fragile] Don't rush to add this check, figure out the real issue if we run into problems.
      //  - if (subscription.isOwnerActive)
      subscription.kill()
    }

    maybeSubscription = None
  }
}
