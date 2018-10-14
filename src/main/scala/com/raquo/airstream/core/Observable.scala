package com.raquo.airstream.core

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.FlattenStrategy
import com.raquo.airstream.features.FlattenStrategy.SwitchStreamStrategy
import com.raquo.airstream.ownership.{Owned, Owner}
import com.raquo.airstream.state.State

import scala.scalajs.js
import scala.util.Try

/** This trait represents a reactive value that can be subscribed to. */
trait Observable[+A] {

  type Self[+T] <: Observable[T]

  protected[airstream] val topoRank: Int

  /** Note: Observer can be added more than once to an Observable.
    * If so, it will observe each event as many times as it was added.
    */
  protected[this] lazy val externalObservers: js.Array[Observer[A]] = js.Array()

  /** Note: This is enforced to be a Set outside of the type system #performance */
  protected[this] val internalObservers: js.Array[InternalObserver[A]] = js.Array()

  /** Get a lazy observable that emits the same values as this one (assuming it has observers, as usual)
    *
    * This is useful when you want to map over an Observable of an unknown type.
    *
    * Note: [[Observable]] itself has no "map" method because mapping over [[State]]
    * without expecting / accounting for State's strictness can result in memory leaks. See README.
    */
  def toLazy: LazyObservable[A]

  /** Create an external observer from a function and subscribe it to this observable.
    *
    * Note: since you won't have a reference to the observer, you will need to call Subscription.kill() to unsubscribe
    * */
  def foreach(onNext: A => Unit)(implicit owner: Owner): Subscription = {
    val observer = Observer(onNext)
    addObserver(observer)(owner)
  }

  /** Subscribe an external observer to this observable */
  def addObserver(observer: Observer[A])(implicit owner: Owner): Subscription = {
    val subscription = Subscription(observer, this, owner)
    externalObservers.push(observer)
    //dom.console.log(s"Adding subscription: $subscription")
    subscription
  }

  // @TODO[Bug] See https://github.com/raquo/Airstream/issues/10
  /** Schedule unsubscription from an external observer in the next transaction.
    *
    * This will still happen synchronously, but will not interfere with
    * iteration over the observables' lists of observers during the current
    * transaction.
    *
    * Note: To completely unsubscribe an Observer from this Observable,
    * you need to remove it as many times as you added it to this Observable.
    */
  @inline def removeObserver(observer: Observer[A]): Unit = {
    Transaction.removeExternalObserver(this, observer)
  }

  // @TODO Why does simple "protected" not work? Specialization?
  /** Child observables call this to announce their existence once they are started */
  protected[airstream] def addInternalObserver(observer: InternalObserver[A]): Unit = {
    internalObservers.push(observer)
  }

//  @inline protected[airstream] def removeInternalObserver(observer: InternalObserver[A]): Unit = {
//    Transaction.removeInternalObserver(observable = this, observer)
//  }

  protected[airstream] def removeInternalObserverNow(observer: InternalObserver[A]): Boolean = {
    val index = internalObservers.indexOf(observer)
    val shouldRemove = index != -1
    if (shouldRemove) {
      internalObservers.splice(index, deleteCount = 1)
    }
    shouldRemove
  }

  /** @return whether observer was removed (`false` if it wasn't subscribed to this observable) */
  protected[airstream] def removeExternalObserverNow(observer: Observer[A]): Boolean = {
    val index = externalObservers.indexOf(observer)
    val shouldRemove = index != -1
    if (shouldRemove) {
      externalObservers.splice(index, deleteCount = 1)
    }
    shouldRemove
  }

  /** This method is fired when this observable starts working (listening for parent events and/or firing its own events)
    *
    * The above semantic is universal, but different observables fit [[onStart]] differently in their lifecycle:
    * - [[State]] is an eager observable, it calls [[onStart]] on initialization.
    * - [[LazyObservable]] (Stream or Signal) calls [[onStart]] when it gets its first observer (internal or external)
    *
    * So, a [[State]] observable calls [[onStart]] only once in its existence, whereas a [[LazyObservable]] calls it
    * potentially multiple times, the second time being after it has stopped (see [[onStop]]).
    */
  @inline protected[this] def onStart(): Unit = ()

  /** This method is fired when this observable stops working (listening for parent events and/or firing its own events)
    *
    * The above semantic is universal, but different observables fit [[onStop]] differently in their lifecycle:
    * - [[State]] is an eager, [[Owned]] observable. It calls [[onStop]] when its [[Owner]] decides to [[State.kill]] it
    * - [[LazyObservable]] (Stream or Signal) calls [[onStop]] when it loses its last observer (internal or external)
    *
    * So, a [[State]] observable calls [[onStop]] only once in its existence. After that, the observable is disabled and
    * will never start working again. On the other hand, a [[LazyObservable]] calls [[onStop]] potentially multiple
    * times, the second time being after it has started again (see [[onStart]]).
    */
  @inline protected[this] def onStop(): Unit = ()

  // === A note on performance with error handling ===
  //
  // MemoryObservables remember their current value as Try[A], whereas
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
  // implementations in EventStream and MemoryObservable are somewhat
  // redundant (non-DRY), but performance friendly.
  //
  // You must be careful when overriding these methods however, as you
  // don't know which one of them will be called, but they need to be
  // implemented to produce similar results

  protected[this] def fireValue(nextValue: A, transaction: Transaction): Unit

  protected[this] def fireError(nextError: Throwable, transaction: Transaction): Unit

  protected[this] def fireTry(nextValue: Try[A], transaction: Transaction): Unit
}

object Observable {

  implicit val switchStreamStrategy: FlattenStrategy[Observable, EventStream, EventStream] = SwitchStreamStrategy

  /** `flatMap` method can be found in [[LazyObservable.MetaLazyObservable]] */
  implicit class MetaObservable[A, Outer[+_] <: Observable[_], Inner[_]](
    val parent: Outer[Inner[A]]
  ) extends AnyVal {

    @inline def flatten[Output[+_] <: LazyObservable[_]](
      implicit strategy: FlattenStrategy[Outer, Inner, Output]
    ): Output[A] = {
      strategy.flatten(parent)
    }
  }
}
