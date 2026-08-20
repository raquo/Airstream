package com.raquo.airstream.state

import com.raquo.airstream.core.WritableSignal
import com.raquo.airstream.split.KeyedStrictSignal

import scala.util.{Success, Try}

// #nc should Val be StaticSource?
class Val[A](constantValue: Try[A]) extends WritableSignal[A] with StrictSignal[A] {

  override protected val topoRank: Int = 1

  // #TODO[API] It would be nice to make this method available on any StrictSignal, not just Val,
  //  but need to be careful to check that the implementation of KeyedStrictSignal.map / distinct
  //  would make sense for any StrictSignal. Can we refactor that with `KeyedStrictSignal` wrapping
  //  StrictSignal (or even Signal) instead of inheriting it? Not sure how to implement that though.
  def keyedWith[K](key: K): Val[A] with KeyedStrictSignal[K, A] = {
    val _key = key
    new Val[A](constantValue) with KeyedStrictSignal[K, A] {
      override val key: K = _key
    }
  }

  /** Value never changes, so we can use a simplified implementation */
  override def tryNow(): Try[A] = constantValue

  override protected def currentValueFromParent(): Try[A] = constantValue

  override protected def onWillStart(): Unit = () // noop
}

object Val {

  def apply[A](value: A): Val[A] = fromTry(Success(value))

  def keyed[K, A](key: K, value: A): Val[A] with KeyedStrictSignal[K, A] = {
    val _key = key
    new Val[A](Success(value)) with KeyedStrictSignal[K, A] {
      override val key: K = _key
    }
  }

  @inline def fromTry[A](value: Try[A]): Val[A] = new Val(value)

  @inline def fromEither[A](value: Either[Throwable, A]): Val[A] = new Val(value.toTry)
}
