package com.raquo.airstream.signal

import scala.util.{Success, Try}

class Val[A](value: Try[A]) extends Signal[A] {

  override protected[airstream] val topoRank: Int = 1

  override protected[this] val initialValue: Try[A] = value

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
