package com.raquo.airstream.core

import com.raquo.airstream.ownership.{ Owner, Subscription }

import scala.scalajs.js

trait SubscribableObservable[A] {

  /** Note: Observer can be added more than once to an Observable.
    * If so, it will observe each event as many times as it was added.
    */
  protected val externalObservers: ObserverList[Observer[A]] = new ObserverList(js.Array())

  /** Note: This is enforced to be a Set outside of the type system #performance */
  protected val internalObservers: ObserverList[InternalObserver[A]] = new ObserverList(js.Array())

  /** Subscribe an external observer to this observable */
  protected def _addExternalObserver(observable: WritableObservable[A], observer: Observer[A])(implicit owner: Owner): Subscription = {
    val subscription = new Subscription(owner, () => Transaction.removeExternalObserver(observable, observer))
    externalObservers.push(observer)
    //dom.console.log(s"Adding subscription: $subscription")
    subscription
  }

  /** Child observable should call this method on its parents when it is started.
    * This observable calls [[onStart]] if this action has given it its first observer (internal or external).
    */
  protected def _addInternalObserver(observer: InternalObserver[A]): Unit = {
    internalObservers.push(observer)
  }


  /** Child observable should call Transaction.removeInternalObserver(parent, childInternalObserver) when it is stopped.
    * This observable calls [[onStop]] if this action has removed its last observer (internal or external).
    */
  protected[airstream] def _removeInternalObserverNow(observer: InternalObserver[A]): Boolean = {
    internalObservers.removeObserverNow(observer.asInstanceOf[InternalObserver[Any]])
  }

  protected[airstream] def _removeExternalObserverNow(observer: Observer[A]): Boolean = {
    externalObservers.removeObserverNow(observer.asInstanceOf[Observer[Any]])
  }

  protected def numAllObservers: Int = externalObservers.length + internalObservers.length


}
