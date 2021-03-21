package com.raquo.airstream.core

import com.raquo.airstream.ownership.{ Owner, Subscription }

import scala.scalajs.js
import scala.collection.mutable

object ObserverRegistry {

  /** Note: Observer can be added more than once to an Observable.
    * If so, it will observe each event as many times as it was added.
    */
  private val _externalObservers: mutable.Map[WritableObservable[Any], ObserverList[Observer[Any]]] = mutable.Map.empty // new ObserverList(js.Array())

  /** Note: This is enforced to be a Set outside of the type system #performance */
  private val _internalObservers: mutable.Map[WritableObservable[Any], ObserverList[InternalObserver[Any]]] = mutable.Map.empty // = new ObserverList(js.Array())

  final def getExternalObservers[A](observable: WritableObservable[A]): ObserverList[Observer[Any]] = {
    _externalObservers.getOrElse(observable.asInstanceOf[WritableObservable[Any]], new ObserverList(js.Array()))
  }

  final def getInternalObservers[A](observable: WritableObservable[A]): ObserverList[InternalObserver[Any]] = {
    _internalObservers.getOrElse(observable.asInstanceOf[WritableObservable[Any]], new ObserverList(js.Array()))
  }


    /** Subscribe an external observer to this observable */
  def addObserver[A](observable: WritableObservable[A], observer: Observer[A])(implicit owner: Owner): Subscription = {
    val subscription = new Subscription(owner, () => Transaction.removeExternalObserver(observable, observer))
    val externalObservers = (_externalObservers.get(observable.asInstanceOf[WritableObservable[Any]]) match {
      case Some(obs) => obs
      case None =>
        val obs = new ObserverList[Observer[Any]](js.Array())
        _externalObservers.update(observable.asInstanceOf[WritableObservable[Any]], obs)
        obs
    }).asInstanceOf[ObserverList[Observer[A]]]
    externalObservers.push(observer)
    //dom.console.log(s"Adding subscription: $subscription")
    subscription
  }

  /** Child observable should call this method on its parents when it is started.
    * This observable calls [[onStart]] if this action has given it its first observer (internal or external).
    */
  def addInternalObserver[A](observable: WritableObservable[A], observer: InternalObserver[A]): Unit = {
    // @TODO Why does simple "protected" not work? Specialization?
    val internalObservers = (_internalObservers.get(observable.asInstanceOf[WritableObservable[Any]]) match {
      case Some(obs) => obs
      case None =>
        val obs = new ObserverList[InternalObserver[Any]](js.Array())
        _internalObservers.update(observable.asInstanceOf[WritableObservable[Any]], obs)
        obs
    }).asInstanceOf[ObserverList[InternalObserver[A]]]

    internalObservers.push(observer)
  }


  /** Child observable should call Transaction.removeInternalObserver(parent, childInternalObserver) when it is stopped.
    * This observable calls [[onStop]] if this action has removed its last observer (internal or external).
    */
  def removeInternalObserverNow[A](observable: WritableObservable[A], observer: InternalObserver[A]): Boolean = {
    _internalObservers.get(observable.asInstanceOf[WritableObservable[Any]]) match {
      case None => false
      case Some(internalObservers) =>
        internalObservers.removeObserverNow(observer.asInstanceOf[InternalObserver[Any]])
    }
  }

  def removeExternalObserverNow[A](observable: WritableObservable[A], observer: Observer[A]): Boolean = {
    _externalObservers.get(observable.asInstanceOf[WritableObservable[Any]]) match {
      case None => false
      case Some(externalObservers) =>
        externalObservers.removeObserverNow(observer.asInstanceOf[Observer[Any]])
    }
  }

  def numAllObservers[A](observable: WritableObservable[A]): Int =
    _externalObservers.get(observable.asInstanceOf[WritableObservable[Any]]).fold(0)(_.length) +
      _internalObservers.get(observable.asInstanceOf[WritableObservable[Any]]).fold(0)(_.length)

}
