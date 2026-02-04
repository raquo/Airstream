package com.raquo.airstream.split

import com.raquo.airstream.core.CoreOps
import com.raquo.airstream.distinct.{DistinctOps, DistinctSignal}
import com.raquo.airstream.misc.MapSignal
import com.raquo.airstream.state.StrictSignal

import scala.util.Try

trait KeyedStrictSignal[+K, +A]
extends StrictSignal[A]
with CoreOps[({ type Self[+V] = KeyedStrictSignal[K, V] })#Self, A] // #TODO[Scala] how to express this more concisely?
with DistinctOps[KeyedStrictSignal[K, A], A] { self =>

  val key: K

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
