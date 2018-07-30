package com.raquo.airstream.signal

import com.raquo.airstream.core.{LazyObservable, MemoryObservable}
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.state.{MapState, State}

import scala.concurrent.Future
import scala.scalajs.js

// @TODO[Integrity] Careful with multiple inheritance & addObserver here
/** Signal is a lazy observable with a current value */
trait Signal[+A] extends MemoryObservable[A] with LazyObservable[A] {

  override type Self[+T] = Signal[T]

  protected[this] var maybeLastSeenCurrentValue: js.UndefOr[A] = js.undefined

  def toState(implicit owner: Owner): State[A] = {
    new MapState[A, A](parent = this, project = identity, owner)
  }

  override def map[B](project: A => B): Signal[B] = {
    new MapSignal(parent = this, project)
  }

  def compose[B](operator: Signal[A] => Signal[B]): Signal[B] = {
    operator(this)
  }

  def combineWith[AA >: A, B](otherSignal: Signal[B]): CombineSignal2[AA, B, (AA, B)] = {
    new CombineSignal2(
      parent1 = this,
      parent2 = otherSignal,
      combinator = (_, _)
    )
  }

  def fold[B](makeInitial: A => B)(fn: (B, A) => B): Signal[B] = {
    new FoldSignal(
      parent = this,
      makeInitialValue = () => makeInitial(now()),
      fn
    )
  }

  /** Initial value is only evaluated if/when needed (when there are observers) */
  override protected[airstream] def now(): A = {
    maybeLastSeenCurrentValue.getOrElse {
      val currentValue = initialValue
      setCurrentValue(currentValue)
      currentValue
    }
  }

  override protected[this] def setCurrentValue(newValue: A): Unit = {
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
    now() // trigger setCurrentValue if we didn't initialize this before
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
