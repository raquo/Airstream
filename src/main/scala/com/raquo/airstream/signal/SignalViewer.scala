package com.raquo.airstream.signal

import com.raquo.airstream.core.{Observer, Subscription}
import com.raquo.airstream.ownership.Owner

import scala.util.Try

class SignalViewer[+A](signal: Signal[A], owner: Owner) {

  private[this] val subscription: Subscription = signal.addObserver(Observer.empty)(owner)

  @inline def now(): A = signal.now()

  @inline def tryNow(): Try[A] = signal.tryNow()

  def kill(): Unit = subscription.kill()
}
