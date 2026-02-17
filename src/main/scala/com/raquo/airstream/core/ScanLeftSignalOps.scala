package com.raquo.airstream.core

import scala.util.Try

trait ScanLeftSignalOps[+Self[+_] <: Signal[_], +A] {

  /** A signal that emits the accumulated value every time that the parent signal emits.
    *
    * @param makeInitial Note: guarded against exceptions
    * @param fn Note: guarded against exceptions
    */
  def scanLeft[B](makeInitial: A => B)(fn: (B, A) => B): Self[B] = {
    scanLeftRecover(parentInitial => parentInitial.map(makeInitial)) { (currentValue, nextParentValue) =>
      Try(fn(currentValue.get, nextParentValue.get))
    }
  }

  /** A signal that emits the accumulated value every time that the parent signal emits.
    *
    * @param makeInitial currentParentValue => initialValue   Note: must not throw
    * @param fn (currentValue, nextParentValue) => nextValue
    * @return
    */
  def scanLeftRecover[B](
    makeInitial: Try[A] => Try[B]
  )(
    fn: (Try[B], Try[A]) => Try[B]
  ): Self[B]
}
