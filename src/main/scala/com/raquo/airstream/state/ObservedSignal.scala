package com.raquo.airstream.state

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.misc.MapSignal
import com.raquo.airstream.ownership.{Owner, Subscription}

/** This class adds a noop observer to `signal`, ensuring that its current value is computed.
  * It then lets you query `signal`'s current value with `now` and `tryNow` methods (see StrictSignal),
  * as well as kill the subscription (see OwnedSignal)
  */
class ObservedSignal[A](
  override val parent: Signal[A],
  observer: Observer[A],
  owner: Owner
) extends MapSignal[A, A](
  parent,
  project = identity,
  recover = None
) with OwnedSignal[A] {

  override protected[this] val subscription: Subscription = addObserver(observer)(owner)
}
