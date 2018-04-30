package com.raquo.airstream.core

import com.raquo.airstream.eventstream.{EventStream, MapEventStream}
import com.raquo.airstream.ownership.Owner

/** An observable that remembers its current value */
trait MemoryObservable[+A] extends Observable[A] {

  /** We don't want public access to this for lazy MemoryObservables (Signals) because if you called .now() on a Signal,
    * you wouldn't know if you were getting the updated value or not, because that would depend on whether said Signal
    * has observers.
    *
    * Use EventStream.sample and EventStream.withCurrentValueOf if you need this on a Signal.
    */
  protected[airstream] def now(): A

  /** Evaluate initial value of this [[MemoryObservable]].
    * This method must only be called once, when this value is first needed.
    * You should override this method as `def` (no `val` or lazy val) to avoid
    * holding a reference to the initial value beyond the duration of its relevance.
    */
  protected[this] def initialValue: A

  /** Update the current value of this [[MemoryObservable]] */
  protected[this] def setCurrentValue(newValue: A): Unit

  def changes: EventStream[A] = new MapEventStream[A, A](parent = this, project = identity)

  /** Note: if you want your observer to only get changes, subscribe to .changes stream instead */
  override def addObserver(observer: Observer[A])(implicit owner: Owner): Subscription = {
    val subscription = super.addObserver(observer)
    observer.onNext(now()) // send current value immediately
    subscription
  }

  /** MemoryObservable propagates only if its value has changed */
  override protected[this] def fire(nextValue: A, transaction: Transaction): Unit = {
    if (nextValue != now()) {
      setCurrentValue(nextValue)
      super.fire(nextValue, transaction)
    }
  }
}
