package com.raquo.airstream.state

import com.raquo.airstream.core.MemoryObservable
import com.raquo.airstream.features.CombineObservable
import com.raquo.airstream.ownership.{Owned, Owner}
import com.raquo.airstream.signal.{FoldSignal, MapSignal, Signal}

import scala.concurrent.Future
import scala.util.{Failure, Try}

/** State is an eager, [[Owned]] observable */
trait State[+A] extends MemoryObservable[A] with Owned {

  override type Self[+T] = State[T]

  protected[state] val owner: Owner

  /** Initial value is evaluated eagerly on initialization */
  protected[this] var currentValue: Try[A] = initialValue

  // State starts itself, it does not need any dependencies to run.
  // Note: This call can be found in every concrete subclass of [[State]].
  //       We don't want it here because of initialization order complications.
  // onStart()

  /** State is evaluated eagerly, so this value will always be consistent, so we can have it public */
  @inline override def tryNow(): Try[A] = currentValue

  /** State is evaluated eagerly, so this value will always be consistent, so we can have it public
    *
    * @throws Exception if current value is an error
    */
  override def now(): A = super.now()

  /** Note: the derived State will have the same Owner as this State.
    * If you want to create a State with a different Owner, use a `.toSignal.toState` sequence
    */
  def map[B](project: A => B): State[B] = {
    new MapState[A, B](parent = this, project, recover = None, owner)
  }

  def combineWith[AA >: A, B](otherState: State[B]): CombineState2[AA, B, (AA, B)] = {
    new CombineState2(
      parent1 = this,
      parent2 = otherState,
      combinator = CombineObservable.guardedCombinator((_, _)),
      owner // @TODO[API] Is this obvious enough?
    )
  }

  /**
    * @param makeInitial Note: guarded against exceptions
    * @param fn Note: guarded against exceptions
    */
  def fold[B](makeInitial: A => B)(fn: (B, A) => B): State[B] = {
    foldRecover(
      parentInitial => parentInitial.map(makeInitial)
    )(
      (currentValue, nextParentValue) => Try(fn(currentValue.get, nextParentValue.get))
    )
  }

  // @TODO[Naming]
  /**
    * @param makeInitial currentParentValue => initialValue
    * @param fn (currentValue, nextParentValue) => nextValue
    * @return
    */
  def foldRecover[B](makeInitial: Try[A] => Try[B])(fn: (Try[B], Try[A]) => Try[B]): State[B] = {
    new FoldSignal(
      parent = this,
      makeInitialValue = () => makeInitial(tryNow()),
      fn
    ).toState(owner)
  }

  /** @param pf Note: guarded against exceptions */
  def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Self[B] = {
    new MapState[A, B](
      parent = this,
      project = identity,
      recover = Some(pf),
      owner = owner
    )
  }

  def recoverIgnoreErrors: Self[A] = recover[A]{ case _ => None }

  def recoverToTry: Self[Try[A]] = map(Try(_)).recover { case err => Some(Failure(err)) }

  @inline def toSignal: Signal[A] = toLazy

  override def toLazy: Signal[A] = {
    new MapSignal[A, A](parent = this, project = identity, recover = None)
  }

  override protected[this] def setCurrentValue(newValue: Try[A]): Unit = {
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
