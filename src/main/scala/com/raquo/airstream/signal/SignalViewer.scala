package com.raquo.airstream.signal

import com.raquo.airstream.basic.MapSignal
import com.raquo.airstream.core.Observer
import com.raquo.airstream.ownership.{Owner, Subscription}

// @TODO[Naming] SignalViewer now *IS* a Signal itself, so it's not a viewer. It's an... ObservedSignal? Or something?

/** This class adds a noop observer to `signal`, ensuring that its current value is computed.
  * It then lets you query `signal`'s current value with `now` and `tryNow` methods (see StrictSignal).
  */
class SignalViewer[+A](
  override val parent: Signal[A], owner: Owner
) extends MapSignal[Any, A](
  parent,
  project = _.asInstanceOf[A], // @TODO[Integrity] Hack around covariance.
  recover = None
) with StrictSignal[A] {

  private[this] val subscription: Subscription = addObserver(Observer.empty)(owner)

  def kill(): Unit = subscription.kill()
}

