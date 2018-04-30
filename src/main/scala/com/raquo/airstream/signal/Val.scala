package com.raquo.airstream.signal

class Val[A](value: A) extends Signal[A] {

  override protected[airstream] val topoRank: Int = 1

  override protected[this] val initialValue: A = value

  /** Public because it is evaluated immediately and never changes */
  override def now(): A = value
}

object Val {

  def apply[A](value: A): Val[A] = new Val(value)
}
