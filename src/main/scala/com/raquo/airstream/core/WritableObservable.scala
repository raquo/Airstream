package com.raquo.airstream.core

import scala.util.Try

trait WritableObservable[A] extends SubscribableObservable[A] {

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

}
