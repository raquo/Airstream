package com.raquo.airstream.core

import scala.util.Try

trait SignalOps[+A] {

  // @TODO[Naming]
  /**
    * @param makeInitial currentParentValue => initialValue   Note: must not throw
    * @param fn (currentValue, nextParentValue) => nextValue
    * @return
    */
  def foldLeftRecover[B](makeInitial: Try[A] => Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B]

  /** @param changesOperator Note: Must not throw!
    * @param initialOperator Note: Must not throw!
    */
  def composeAll[B](
    changesOperator: EventStream[A] => EventStream[B],
    initialOperator: Try[A] => Try[B]
  ): Signal[B]

  def changes: EventStream[A]

  // TODO these used to be protected[airstream]

  /** See comment for [[tryNow]] right above
    *
    * @throws Exception if current value is an error
    */
  def now(): A

  def tryNow(): Try[A]

}
