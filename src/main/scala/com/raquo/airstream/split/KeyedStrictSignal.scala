package com.raquo.airstream.split

import com.raquo.airstream.core.CoreOps
import com.raquo.airstream.distinct.{DistinctOps, DistinctSignal}
import com.raquo.airstream.misc.MapSignal
import com.raquo.airstream.state.StrictSignal

import scala.util.Try

trait KeyedStrictSignal[+K, +A]
extends StrictSignal[A]
with CoreOps[({ type Self[+V] = KeyedStrictSignal[K, V] })#Self, A] // #TODO how to express this more concisely?
with DistinctOps[KeyedStrictSignal[K, A], A] { self =>

  val key: K

  // override abstract def tryNow(): Try[A] = ???

  // #nc Can't figure out how to avoid the redundancy of the below
  //  - all of this is already defined in DistinctOps but I can't extend it twice

  /** Distinct events (but keep all errors) by == (equals) comparison */
  // override def distinct: KeyedStrictSignal[K, A] = distinctByFn(_ == _)
  //
  // /** Distinct events (but keep all errors) by matching key
  //   * Note: `key(event)` might be evaluated more than once for each event
  //   */
  // override def distinctBy(key: A => Any): KeyedStrictSignal[K, A] = distinctByFn(key(_) == key(_))
  //
  // /** Distinct events (but keep all errors) by reference equality (eq) */
  // override def distinctByRef(implicit ev: A <:< AnyRef): KeyedStrictSignal[K, A] = distinctByFn(ev(_) eq ev(_))
  //
  // /** Distinct events (but keep all errors) using a comparison function */
  // override def distinctByFn(isSame: (A, A) => Boolean): KeyedStrictSignal[K, A] = distinctTry {
  //   case (Success(prev), Success(next)) => isSame(prev, next)
  //   case _ => false
  // }
  //
  // /** Distinct errors only (but keep all events) using a comparison function */
  // override def distinctErrors(isSame: (Throwable, Throwable) => Boolean): KeyedStrictSignal[K, A] = distinctTry {
  //   case (Failure(prevErr), Failure(nextErr)) => isSame(prevErr, nextErr)
  //   case _ => false
  // }

  override def map[B](project: A => B): KeyedStrictSignal[K, B] = {
    new MapSignal[A, B](parent = this, project, recover = None)
      with KeyedStrictSignal[K, B] {

      override val key: K = self.key

      override def tryNow(): Try[B] = super.tryNow()
    }
  }

  /** Distinct all values (both events and errors) using a comparison function */
  override def distinctTry(isSame: (Try[A], Try[A]) => Boolean): KeyedStrictSignal[K, A] = {
    new DistinctSignal[A](parent = this, isSame, resetOnStop = false)
      with KeyedStrictSignal[K, A] {

      override val key: K = self.key

      override def tryNow(): Try[A] = super.tryNow()
    }
  }
}
