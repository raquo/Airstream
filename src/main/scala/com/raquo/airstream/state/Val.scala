package com.raquo.airstream.state

import com.raquo.airstream.core.WritableSignal

import scala.util.{ Success, Try }

class Val[A](override protected val initialValue: Try[A]) extends StrictSignal[A] with WritableSignal[A] {

  override protected[airstream] val topoRank: Int = 1

  /** Public because it is evaluated immediately and never changes */
  override def tryNow(): Try[A] = initialValue

  /** Public because it is evaluated immediately and never changes
    *
    * @throws Exception if value is an error
    */
  override def now(): A = initialValue.get
}

object Val {

  def apply[A](value: A): Val[A] = fromTry(Success(value))

  @inline def fromTry[A](value: Try[A]): Val[A] = new Val(value)
}
