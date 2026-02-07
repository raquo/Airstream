package com.raquo.airstream.split

import com.raquo.airstream.core.CoreOps
import com.raquo.airstream.distinct.{DistinctOps, DistinctSignal}
import com.raquo.airstream.misc.MapSignal
import com.raquo.airstream.state.{StrictSignal, WritableStrictSignal}

import scala.util.Try

trait KeyedStrictSignal[+K, +A]
extends StrictSignal[A]
with Keyed[K]
with CoreOps[({ type Self[+V] = KeyedStrictSignal[K, V] })#Self, A] // #TODO[Scala] how to express this more concisely?
with DistinctOps[KeyedStrictSignal[K, A], A] { self =>

  override def map[B](project: A => B): KeyedStrictSignal[K, B] = {
    new MapSignal[A, B](parent = this, project, recover = None)
      with KeyedStrictSignal[K, B]
      with WritableStrictSignal[B] {

      override val key: K = self.key
    }
  }

  /** Distinct all values (both events and errors) using a comparison function
    *
    * See other distinct* operators in [[DistinctOps]]
    */
  override def distinctTry(isSame: (Try[A], Try[A]) => Boolean): KeyedStrictSignal[K, A] = {
    new DistinctSignal[A](parent = this, isSame, resetOnStop = false)
      with KeyedStrictSignal[K, A]
      with WritableStrictSignal[A] {

      override val key: K = self.key
    }
  }
}
