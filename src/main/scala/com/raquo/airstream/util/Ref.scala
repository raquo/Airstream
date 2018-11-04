package com.raquo.airstream.util

/** Wraps a value with desired `equals` semantics to give MemoryObservables of Ref[A]
  * desirable performance or semantic characteristics (see Ref companion object methods for details).
  *
  * Note: This is not a generic replacement of the `Equals` trait. This is only for use in Signals and State.
  *
  * More documentation is coming once I convince myself that this is the right design.
  *
  * @param refEquals Must not throw!
  */
class Ref[+A <: AnyRef](val value: A, refEquals: (Ref[_ <: AnyRef], Ref[_ <: AnyRef]) => Boolean) extends Equals {

  // def map[B <: AnyRef](project: A => B): Ref[B] = new Ref(project(value), refEquals)

  override def canEqual(that: Any): Boolean = that.isInstanceOf[Ref[_]]

  override def equals(obj: Any): Boolean = {
    obj match {
      case ref: Ref[_] => refEquals(this, ref)
      case _ => false
    }
  }

  override def toString: String = s"Ref(${value.toString})"
}

object Ref {

  private val refEqEquals: (Ref[_ <: AnyRef], Ref[_ <: AnyRef]) => Boolean = (ref1, ref2) => ref1.value eq ref2.value

  private val refNeverEquals: (Ref[_ <: AnyRef], Ref[_ <: AnyRef]) => Boolean = (_, _) => false

  /** A Ref that is only equal if the values match referentially
    *
    * Use for MemoryObservables of large immutable Maps or Lists to improve
    * performance (otherwise their equals method would iterate through the
    * whole map / list to find differences)
    */
  def ref[A <: AnyRef](value: A): Ref[A] = new Ref(value, refEqEquals)

  // @TODO[API] Perhaps we should relax this a bit, returning true if ref1 eq ref2? (by reference).
  /** A Ref that is never equal to anything, including itself.
    *
    * Use for MemoryObservables of mutable values (I mean, if your observable
    * emits the same mutable value when it updates, instead of emitting a new
    * immutable value)
    */
  def never[A <: AnyRef](value: A): Ref[A] = new Ref(value, refNeverEquals)

}
