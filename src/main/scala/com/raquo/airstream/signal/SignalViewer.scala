package com.raquo.airstream.signal

import com.raquo.airstream.core.{Observer, Subscription}
import com.raquo.airstream.ownership.Owner

import scala.util.Try

/** This class adds a noop observer to `signal`, ensuring that its current value is computed.
  * It then lets you query `signal`'s current value with `now` and `tryNow` methods.
  *
  * This will likely replace the entirety of the `State` type in Airstream.
  */
class SignalViewer[+A](signal: Signal[A], owner: Owner) {

  private[this] val subscription: Subscription = signal.addObserver(Observer.empty)(owner)

  @inline def now(): A = signal.now()

  @inline def tryNow(): Try[A] = signal.tryNow()

  def kill(): Unit = subscription.kill()
}
