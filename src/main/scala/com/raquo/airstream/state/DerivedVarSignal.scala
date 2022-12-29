package com.raquo.airstream.state

import com.raquo.airstream.core.Observer
import com.raquo.airstream.misc.MapSignal
import com.raquo.airstream.ownership.{Owner, Subscription}

class DerivedVarSignal[A, B](
  parent: Var[A],
  zoomIn: A => B,
  owner: Owner
) extends MapSignal[A, B](
  parent.signal,
  project = zoomIn,
  recover = None
) with OwnedSignal[B] {

  // Note that even if owner kills subscription, this signal might remain due to other listeners
  override protected[state] def isStarted: Boolean = super.isStarted

  override protected[this] val subscription: Subscription = this.addObserver(Observer.empty)(owner)
}
