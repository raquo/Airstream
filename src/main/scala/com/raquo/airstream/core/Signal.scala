package com.raquo.airstream.core

import com.raquo.airstream.combine.CombineSignalN
import com.raquo.airstream.combine.generated.{CombinableSignal, StaticSignalCombineOps}
import com.raquo.airstream.core.Source.SignalSource
import com.raquo.airstream.custom.CustomSource._
import com.raquo.airstream.custom.{CustomSignalSource, CustomSource}
import com.raquo.airstream.debug.{DebuggableSignal, Debugger, DebuggerSignal}
import com.raquo.airstream.misc.generated._
import com.raquo.airstream.misc.{FoldLeftSignal, MapEventStream, MapSignal}
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.split.{SplittableOneSignal, SplittableSignal}
import com.raquo.airstream.state.{ObservedSignal, OwnedSignal, Val}
import com.raquo.airstream.timing.FutureSignal

import scala.annotation.unused
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Failure, Try}

/** Signal is an Observable with a current value. */
trait Signal[+A] extends Observable[A] with BaseObservable[Signal, A] with SignalSource[A] {

  /** Get the signal's current value */
  protected[airstream] def tryNow(): Try[A]

  /** Get the signal's current value
    *
    * @throws the signal's error if its current value is an error
    */
  protected[airstream] def now(): A = tryNow().get

  /** @param project Note: guarded against exceptions */
  override def map[B](project: A => B): Signal[B] = {
    new MapSignal(parent = this, project, recover = None)
  }

  /** @param operator Note: Must not throw! */
  def compose[B](operator: Signal[A] => Signal[B]): Signal[B] = {
    operator(this)
  }

  /** @param operator Note: Must not throw! */
  def composeChanges[AA >: A](
    operator: EventStream[A] => EventStream[AA]
  ): Signal[AA] = {
    composeAll(operator, initialOperator = identity)
  }

  /** @param changesOperator Note: Must not throw!
    * @param initialOperator Note: Must not throw!
    */
  def composeAll[B](
    changesOperator: EventStream[A] => EventStream[B],
    initialOperator: Try[A] => Try[B]
  ): Signal[B] = {
    changesOperator(changes).toSignalWithTry(initialOperator(tryNow()))
  }

  def changes: EventStream[A] = new MapEventStream[A, A](parent = this, project = identity, recover = None)

  /**
    * @param makeInitial Note: guarded against exceptions
    * @param fn Note: guarded against exceptions
    */
  def foldLeft[B](makeInitial: A => B)(fn: (B, A) => B): Signal[B] = {
    foldLeftRecover(
      parentInitial => parentInitial.map(makeInitial)
    )(
      (currentValue, nextParentValue) => Try(fn(currentValue.get, nextParentValue.get))
    )
  }

  // @TODO[Naming]
  /**
    * @param makeInitial currentParentValue => initialValue   Note: must not throw
    * @param fn (currentValue, nextParentValue) => nextValue
    * @return
    */
  def foldLeftRecover[B](makeInitial: Try[A] => Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    new FoldLeftSignal(
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

  /** See also debug methods in [[com.raquo.airstream.debug.DebuggableObservable]] */
  override def debugWith(debugger: Debugger[A]): Signal[A] = {
    new DebuggerSignal[A](this, debugger)
  }

  /** Add a noop observer to this signal to ensure that it's started.
    * This lets you access .now and .tryNow on the resulting StrictSignal.
    *
    * You can use `myStream.toWeakSignal.observe.tryNow()` to read the last emitted
    * value from event streams just as well.
    */
  def observe(implicit owner: Owner): OwnedSignal[A] = new ObservedSignal(this, Observer.empty, owner)

  override def toObservable: Signal[A] = this

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

  // @TODO[API] Use pattern match instead when isInstanceOf performance is fixed: https://github.com/scala-js/scala-js/issues/3815
  override protected def onAddedExternalObserver(observer: Observer[A]): Unit = {
    super.onAddedExternalObserver(observer)
    observer.onTry(tryNow()) // send current value immediately
  }

}

object Signal {

  def fromValue[A](value: A): Val[A] = Val(value)

  def fromTry[A](value: Try[A]): Val[A] = Val.fromTry(value)

  def fromFuture[A](future: Future[A]): Signal[Option[A]] = {
    new FutureSignal(future)
  }

  def fromJsPromise[A](promise: js.Promise[A]): Signal[Option[A]] = {
    new FutureSignal(promise.toFuture)
  }

  /** Easy helper for custom signals. See [[CustomSignalSource]] for docs.
    *
    * @param stop MUST NOT THROW!
    */
  def fromCustomSource[A](
    initial: => A,
    start: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => Unit,
    stop: StartIndex => Unit
  ): Signal[A] = {
    CustomSignalSource[A](initial)( (setValue, getValue, getStartIndex, getIsStarted) => {
      CustomSource.Config(
        onStart = () => start(setValue, getValue, getStartIndex, getIsStarted),
        onStop = () => stop(getStartIndex())
      )
    })
  }

  def sequence[A](
    signals: Seq[Signal[A]]
  ): Signal[Seq[A]] = {
    new CombineSignalN[A, Seq[A]](signals, identity)
  }

  @inline def combineSeq[A](signals: Seq[Signal[A]]): Signal[Seq[A]] = sequence(signals)

  /** Provides methods on Signal companion object: combine, combineWith */
  implicit def toSignalCompanionCombineSyntax(@unused s: Signal.type): StaticSignalCombineOps.type = StaticSignalCombineOps

  /** Provides methods on Signal: combine, combineWith, withCurrentValueOf, sample */
  implicit def toCombinableSignal[A](signal: Signal[A]): CombinableSignal[A] = new CombinableSignal(signal)

  /** Provides methods on Signal: split, splitIntoSignals */
  implicit def toSplittableSignal[M[_], Input](signal: Signal[M[Input]]): SplittableSignal[M, Input] = new SplittableSignal(signal)

  /** Provides methods on Signal: splitOne, splitOneIntoSignals */
  implicit def toSplittableOneSignal[A](signal: Signal[A]): SplittableOneSignal[A] = new SplittableOneSignal[A](signal)

  /** Provides signal-specific debug* methods: debugSpyInitialEval, debugLogInitialEval, debugBreakInitialEval */
  implicit def toDebuggableSignal[A](signal: Signal[A]): DebuggableSignal[A] = new DebuggableSignal[A](signal)

  // toTupleSignalN conversions provide mapN method on Signals of tuples

  implicit def toTupleSignal2[T1, T2](stream: Signal[(T1, T2)]): TupleSignal2[T1, T2] = new TupleSignal2(stream)

  implicit def toTupleSignal3[T1, T2, T3](stream: Signal[(T1, T2, T3)]): TupleSignal3[T1, T2, T3] = new TupleSignal3(stream)

  implicit def toTupleSignal4[T1, T2, T3, T4](stream: Signal[(T1, T2, T3, T4)]): TupleSignal4[T1, T2, T3, T4] = new TupleSignal4(stream)

  implicit def toTupleSignal5[T1, T2, T3, T4, T5](stream: Signal[(T1, T2, T3, T4, T5)]): TupleSignal5[T1, T2, T3, T4, T5] = new TupleSignal5(stream)

  implicit def toTupleSignal6[T1, T2, T3, T4, T5, T6](stream: Signal[(T1, T2, T3, T4, T5, T6)]): TupleSignal6[T1, T2, T3, T4, T5, T6] = new TupleSignal6(stream)

  implicit def toTupleSignal7[T1, T2, T3, T4, T5, T6, T7](stream: Signal[(T1, T2, T3, T4, T5, T6, T7)]): TupleSignal7[T1, T2, T3, T4, T5, T6, T7] = new TupleSignal7(stream)

  implicit def toTupleSignal8[T1, T2, T3, T4, T5, T6, T7, T8](stream: Signal[(T1, T2, T3, T4, T5, T6, T7, T8)]): TupleSignal8[T1, T2, T3, T4, T5, T6, T7, T8] = new TupleSignal8(stream)

  implicit def toTupleSignal9[T1, T2, T3, T4, T5, T6, T7, T8, T9](stream: Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]): TupleSignal9[T1, T2, T3, T4, T5, T6, T7, T8, T9] = new TupleSignal9(stream)
}
