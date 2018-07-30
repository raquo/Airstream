package com.raquo.airstream.state

import com.raquo.airstream.core.MemoryObservable
import com.raquo.airstream.ownership.{Owned, Owner}
import com.raquo.airstream.signal.{FoldSignal, MapSignal, Signal}

import scala.concurrent.Future

/** State is an eager, [[Owned]] observable */
trait State[+A] extends MemoryObservable[A] with Owned {

  override type Self[+T] = State[T]

  protected[state] val owner: Owner

  /** Initial value is evaluated eagerly on initialization */
  protected[this] var currentValue: A = initialValue

  // State starts itself, it does not need any dependencies to run.
  // Note: This call can be found in every concrete subclass of [[State]].
  //       We don't want it here because of initialization order complications.
  // onStart()

  /** State is evaluated eagerly, so this value will always be consistent, so we can have it public */
  @inline override def now(): A = currentValue

  /** Note: the derived State will have the same Owner as this State.
    * If you want to create a State with a different Owner, use a `.toSignal.toState` sequence
    */
  def map[B](project: A => B): State[B] = {
    new MapState[A, B](parent = this, project, owner)
  }

  def combineWith[AA >: A, B](otherState: State[B]): CombineState2[AA, B, (AA, B)] = {
    new CombineState2(
      parent1 = this,
      parent2 = otherState,
      combinator = (_, _),
      owner // @TODO[API] Is this obvious enough?
    )
  }

  def fold[B](makeInitial: A => B)(fn: (B, A) => B): State[B] = {
    new FoldSignal(
      parent = this,
      makeInitialValue = () => makeInitial(now()),
      fn
    ).toState(owner)
  }

  @inline def toSignal: Signal[A] = toLazy

  override def toLazy: Signal[A] = {
    new MapSignal[A, A](parent = this, project = identity)
  }

  override protected[this] def setCurrentValue(newValue: A): Unit = {
    currentValue = newValue
  }

  override protected[this] def onKilled(): Unit = {
    onStop() // @TODO[Integrity] onStop must not be called anywhere else on State observables. Possible to enforce that?
  }
}

object State {

  @inline def fromFuture[A](future: Future[A])(implicit owner: Owner): State[Option[A]] = {
    Signal.fromFuture(future).toState(owner)
  }

  implicit def toTuple2State[A, B](state: State[(A, B)]): Tuple2State[A, B] = {
    new Tuple2State(state)
  }
}
