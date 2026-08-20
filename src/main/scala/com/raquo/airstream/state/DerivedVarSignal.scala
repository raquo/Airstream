package com.raquo.airstream.state

import com.raquo.airstream.core.Observer
import com.raquo.airstream.map.MapSignal
import com.raquo.airstream.ownership.{Owner, Subscription}

class DerivedVarSignal[A, B](
  parent: Var[A],
  zoomIn: A => B,
  owner: Owner,
  parentDisplayName: => String
)
extends MapSignal[A, B](
  parent.signal,
  project = zoomIn,
  recover = None
)
with OwnedSignal[B]
with WritableStrictSignal[B] {

  // Note that even if owner kills subscription, this signal might remain due to other listeners
  override protected[state] def isStarted: Boolean = super.isStarted

  // #TODO[API] Should we switch this `Observer.empty` to `Observer.emptyIgnoreErrors`?
  //  - I'm not sure why we want to send errors into unhandled – even if there are other observers
  //  - I think we've changed our ideas about this once we started leaning more into lazy vars
  //  - But this var is not lazy, it's strict, so... needs a review.
  override protected[this] val subscription: Subscription = this.addObserver(Observer.empty)(owner)

  override protected def defaultDisplayName: String = parentDisplayName + ".signal"
}
