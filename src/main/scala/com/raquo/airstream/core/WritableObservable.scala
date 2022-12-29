package com.raquo.airstream.core

import com.raquo.airstream.ownership.{Owner, Subscription}
import com.raquo.ew.JsArray

import scala.util.Try

trait WritableObservable[A] extends Observable[A] {

  // === A note on performance with error handling ===
  //
  // Signals remember their current value as Try[A], whereas
  // EventStream-s normally fire plain A values and do not need them
  // wrapped in Try. To make things more complicated, user-provided
  // callbacks like `project` in `.map(project)` need to be wrapped in
  // Try() for safety.
  //
  // A worst case performance scenario would see Airstream constantly
  // wrapping and unwrapping the values being propagated, initializing
  // many Success() objects as we walk along the observables dependency
  // graph.
  //
  // We avoid this by keeping the values unwrapped as much as possible
  // in event streams, but wrapping them in signals and state. When
  // switching between streams and memory observables and vice versa
  // we have to pay a small price to wrap or unwrap the value. It's a
  // miniscule penalty that doesn't matter, but if you're wondering
  // how we decide whether to implement onTry or onNext+onError in a
  // particular InternalObserver, this is one of the main factors.
  //
  // With this in mind, you can see fireValue / fireError / fireTry
  // implementations in EventStream and Signal are somewhat
  // redundant (non-DRY), but performance friendly.
  //
  // You must be careful when overriding these methods however, as you
  // don't know which one of them will be called, but they need to be
  // implemented to produce similar results

  protected def fireValue(nextValue: A, transaction: Transaction): Unit

  protected def fireError(nextError: Throwable, transaction: Transaction): Unit

  protected def fireTry(nextValue: Try[A], transaction: Transaction): Unit


  /** Note: Observer can be added more than once to an Observable.
    * If so, it will observe each event as many times as it was added.
    */
  protected val externalObservers: ObserverList[Observer[A]] = new ObserverList(JsArray())

  /** Note: This is enforced to be a Set outside of the type system #performance */
  protected val internalObservers: ObserverList[InternalObserver[A]] = new ObserverList(JsArray())

  override def addObserver(observer: Observer[A])(implicit owner: Owner): Subscription = {
    // #nc[doc] - document this onstart.shared mechanism, both in code and in the real docs.
    //  - also for extenders: this must be called by your observable also if it can get started without external observers downstream (basically, it shouldn't).
    Transaction.onStart.shared {
      maybeWillStart()
      val subscription = addExternalObserver(observer, owner)
      onAddedExternalObserver(observer)
      maybeStart()
      subscription
    }
  }

  /** Subscribe an external observer to this observable */
  override protected[this] def addExternalObserver(observer: Observer[A], owner: Owner): Subscription = {
    val subscription = new Subscription(owner, () => removeExternalObserver(observer))
    externalObservers.push(observer)
    //dom.console.log(s"Adding subscription: $subscription")
    subscription
  }

  /** Child observable should call this method on its parents when it is started.
    * This observable calls [[onStart]] if this action has given it its first observer (internal or external).
    */
  override protected[airstream] def addInternalObserver(observer: InternalObserver[A], shouldCallMaybeWillStart: Boolean): Unit = {
    //println(s"$this > aio   shouldCallMaybeWillStart=$shouldCallMaybeWillStart")
    if (!isStarted && shouldCallMaybeWillStart) {
      maybeWillStart()
    }
    //println(s"$this < aio")
    internalObservers.push(observer)
    maybeStart()
  }

  /** Child observable should call parent.removeInternalObserver(childInternalObserver) when it is stopped.
    * This observable calls [[onStop]] if this action has removed its last observer (internal or external).
    */
  override protected[airstream] def removeInternalObserverNow(observer: InternalObserver[A]): Unit = {
    val removed = internalObservers.removeObserverNow(observer)
    if (removed) {
      maybeStop()
    }
  }

  override protected[airstream] def removeExternalObserverNow(observer: Observer[A]): Unit = {
    val removed = externalObservers.removeObserverNow(observer)
    if (removed) {
      maybeStop()
    }
  }

  private[this] def maybeStart(): Unit = {
    val isStarting = numAllObservers == 1
    if (isStarting) {
      // We've just added first observer
      onStart()
    }
  }

  private[this] def maybeStop(): Unit = {
    if (!isStarted) {
      // We've just removed last observer
      onStop()
    }
  }

  override protected def numAllObservers: Int = externalObservers.length + internalObservers.length
}
