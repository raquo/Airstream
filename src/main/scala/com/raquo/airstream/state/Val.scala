package com.raquo.airstream.state

import com.raquo.airstream.core.WritableSignal

import scala.util.{Success, Try}

class Val[A](constantValue: Try[A]) extends WritableSignal[A] with StrictSignal[A] {

  override protected val topoRank: Int = 1

  /** Value never changes, so we can use a simplified implementation */
  override def tryNow(): Try[A] = constantValue

  override protected def currentValueFromParent(): Try[A] = constantValue

  override protected def onWillStart(): Unit = () // noop
}

object Val {

  def apply[A](value: A): Val[A] = fromTry(Success(value))

  @inline def fromTry[A](value: Try[A]): Val[A] = new Val(value)
}
