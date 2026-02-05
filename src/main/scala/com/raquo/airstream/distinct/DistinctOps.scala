package com.raquo.airstream.distinct

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.split.KeyedStrictSignal
import com.raquo.airstream.state.{StrictSignal, Var}

import scala.util.{Failure, Success, Try}

trait DistinctOps[+Self, +A] {

  /** Distinct events (but keep all errors) by == (equals) comparison */
  def distinct: Self = distinctByFn(_ == _)

  /** Distinct events (but keep all errors) by matching key
    * Note: `key(event)` might be evaluated more than once for each event
    */
  def distinctBy(key: A => Any): Self = distinctByFn(key(_) == key(_))

  /** Distinct events (but keep all errors) by reference equality (eq) */
  def distinctByRef(implicit ev: A <:< AnyRef): Self = distinctByFn(ev(_) eq ev(_))

  /** Distinct events (but keep all errors) using a comparison function */
  def distinctByFn(isSame: (A, A) => Boolean): Self = distinctTry {
    case (Success(prev), Success(next)) => isSame(prev, next)
    case _ => false
  }

  /** Distinct errors only (but keep all events) using a comparison function */
  def distinctErrors(isSame: (Throwable, Throwable) => Boolean): Self = distinctTry {
    case (Failure(prevErr), Failure(nextErr)) => isSame(prevErr, nextErr)
    case _ => false
  }

  /** Distinct all values (both events and errors) using a comparison function
    *
    * Implementations typically instantiate subtypes of [[DistinctStream]] or [[DistinctSignal]] matching the [[Self]] type:
    *  - [[EventStream.distinctTry]]
    *  - [[Signal.distinctTry]]
    *  - [[StrictSignal.distinctTry]]
    *  - [[KeyedStrictSignal.distinctTry]]
    *  - [[Var.distinctTry]]
    *  - [[DistinctOps.F.distinctTry]]
    */
  def distinctTry(isSame: (Try[A], Try[A]) => Boolean): Self
}

object DistinctOps {

  type Distinctor[A] = (Try[A], Try[A]) => Boolean

  type DistinctorF[A] = F[A] => (Try[A], Try[A]) => Boolean

  def DistinctorF[A](f: F[A] => (Try[A], Try[A]) => Boolean): DistinctorF[A] =
    f

  // #nc[split] naming
  class F[A]
  extends DistinctOps[Distinctor[A], A]
  with Distinctor[A] {

    override def distinctTry(isSame: Distinctor[A]): Distinctor[A] =
      isSame

    // This allows us to use instance of `F` itself as a distinctor
    // This lets us pass `identity` to distinctCompose to support pre-v18 split operator syntax.
    override def apply(v1: Try[A], v2: Try[A]): Boolean = false
  }
}
