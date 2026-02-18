package com.raquo.airstream.split

import com.raquo.airstream.core.CoreOps
import com.raquo.airstream.distinct.{DistinctOps, DistinctSignal}
import com.raquo.airstream.misc.MapSignal
import com.raquo.airstream.state.{LazyStrictSignal, StrictSignal}

import scala.util.Try

trait KeyedStrictSignal[+K, +A]
extends StrictSignal[A]
with Keyed[K]
with CoreOps[({ type Self[+V] = KeyedStrictSignal[K, V] })#Self, A] // #TODO[Scala] how to express this more concisely?
with DistinctOps[KeyedStrictSignal[K, A], A] { self =>

  // #Note: Only add operators here that are needed internally for Airstream's split* functionality.
  //  - Retaining .key is not part of the general public user contract.

  override def map[B](project: A => B): KeyedStrictSignal[K, B] = {
    new MapSignal[A, B](parent = this, project, recover = None)
      with KeyedStrictSignal[K, B]
      with LazyStrictSignal[A, B] {

      override val key: K = self.key

      override protected[this] def displayClassName: String = s"KeyedStrictSignal.map(key=${key})"

      override protected val displayNameSuffix: String = ".map"
    }
  }

  /** Distinct all values (both events and errors) using a comparison function
    *
    * See other distinct* operators in [[DistinctOps]]
    */
  override def distinctTry(isSame: (Try[A], Try[A]) => Boolean): KeyedStrictSignal[K, A] = {
    new DistinctSignal[A](parent = this, isSame, resetOnStop = false)
      with KeyedStrictSignal[K, A]
      with LazyStrictSignal[A, A] {

      override val key: K = self.key

      override protected[this] def displayClassName: String = s"KeyedStrictSignal.distinct*(key=${key})"

      override protected val displayNameSuffix: String = ".distinct*"
    }
  }
}

object KeyedStrictSignal {

  /** Use this extractor if you want to name the `key` argument, e.g.:
    * {{{
    * seqSignal.splitSeq(_.id) { case KeyedStrictSignal(signal, id) => ... }
    * }}}
    */
  def unapply[K, A](signal: KeyedStrictSignal[K, A]): Some[(KeyedStrictSignal[K, A], K)] = {
    Some((signal, signal.key))
  }

  /** Use this shorthand extractor if you want to name the `key` argument, e.g.:
    * {{{
    * seqSignal.splitSeq(_.id) { case withKey(signal, id) => ... }
    * }}}
    * Or (gasp!) using infix notation:
    * {{{
    * seqSignal.splitSeq(_.id) { case signal withKey id => ... }
    * }}}
    */
  object withKey {
    def unapply[K, A](signal: KeyedStrictSignal[K, A]): Some[(KeyedStrictSignal[K, A], K)] = {
      Some((signal, signal.key))
    }
  }
}
