package com.raquo.airstream.signal

import com.raquo.airstream.core.{LazyObservable, MemoryObservable}
import com.raquo.airstream.features.CombineObservable
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.state.{MapState, State}

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Failure, Try}

// @TODO[Integrity] Careful with multiple inheritance & addObserver here
/** Signal is a lazy observable with a current value */
trait Signal[+A] extends MemoryObservable[A] with LazyObservable[A] {

  override type Self[+T] = Signal[T]

  protected[this] var maybeLastSeenCurrentValue: js.UndefOr[Try[A]] = js.undefined

  def toState(implicit owner: Owner): State[A] = {
    new MapState[A, A](parent = this, project = identity, recover = None, owner)
  }

  /** @param project Note: guarded against exceptions */
  override def map[B](project: A => B): Signal[B] = {
    new MapSignal(parent = this, project, recover = None)
  }

  /** @param operator Note: Must not throw! */
  def compose[B](operator: Signal[A] => Signal[B]): Signal[B] = {
    operator(this)
  }

  def combineWith[AA >: A, B](otherSignal: Signal[B]): CombineSignal2[AA, B, (AA, B)] = {
    new CombineSignal2(
      parent1 = this,
      parent2 = otherSignal,
      combinator = CombineObservable.guardedCombinator((_, _))
    )
  }


  /**
    * @param makeInitial Note: guarded against exceptions
    * @param fn Note: guarded against exceptions
    */
  def fold[B](makeInitial: A => B)(fn: (B, A) => B): Signal[B] = {
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
  def foldRecover[B](makeInitial: Try[A] => Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    new FoldSignal(
      parent = this,
      makeInitialValue = () => makeInitial(tryNow()),
      fn
    )
  }

  /** @param pf Note: guarded against exceptions */
  override def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Signal[B] = {
    new MapSignal[A, B](
      this,
      project = identity,
      recover = Some(pf)
    )
  }

  override def recoverToTry: Signal[Try[A]] = map(Try(_)).recover[Try[A]] { case err => Some(Failure(err)) }

  /** Initial value is only evaluated if/when needed (when there are observers) */
  override protected[airstream] def tryNow(): Try[A] = {
    maybeLastSeenCurrentValue.getOrElse {
      val currentValue = initialValue
      setCurrentValue(currentValue)
      currentValue
    }
  }

  override protected[this] def setCurrentValue(newValue: Try[A]): Unit = {
    maybeLastSeenCurrentValue = js.defined(newValue)
  }

  /** Here we need to ensure that Signal's default value has been evaluated.
    * It is important because if a Signal gets started by means of its .changes
    * stream acquiring an observer, nothing else would trigger this evaluation
    * because initialValue is not directly used by the .changes stream.
    * However, when the events start coming in, we will need this initialValue
    * because Signal needs to know when its current value has changed.
    */
  override protected[this] def onStart(): Unit = {
    tryNow() // trigger setCurrentValue if we didn't initialize this before
    super.onStart()
  }
}

object Signal {

  @inline def fromFuture[A](future: Future[A]): Signal[Option[A]] = {
    new FutureSignal(future)
  }

  implicit def toTuple2Signal[A, B](signal: Signal[(A, B)]): Tuple2Signal[A, B] = {
    new Tuple2Signal(signal)
  }
}
