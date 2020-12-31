package com.raquo.airstream.signal

import app.tulz.tuplez.Composition
import com.raquo.airstream.combine.CombineSignalN
import com.raquo.airstream.combine.generated.{CombinableSignal, CombineSignal2, SampleCombineSignal2, StaticSignalCombineMethods}
import com.raquo.airstream.core.{AirstreamError, Observable, Observer, Transaction}
import com.raquo.airstream.custom.CustomSource._
import com.raquo.airstream.custom.{CustomSignalSource, CustomSource}
import com.raquo.airstream.eventstream.{EventStream, MapEventStream}
import com.raquo.airstream.ownership.Owner

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** Signal is an Observable with a current value. */
trait Signal[+A] extends Observable[A] {

  override type Self[+T] = Signal[T]

  /** Evaluate initial value of this [[Signal]].
    * This method must only be called once, when this value is first needed.
    * You should override this method as `def` (no `val` or lazy val) to avoid
    * holding a reference to the initial value beyond the duration of its relevance.
    */
  // @TODO[Integrity] ^^^ Does this memory management advice even hold water?
  protected[this] def initialValue: Try[A]

  protected[this] var maybeLastSeenCurrentValue: js.UndefOr[Try[A]] = js.undefined

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

  def combineWith[B, C](otherSignal: Signal[B])(combinator: (A, B) => C): Signal[C] = {
    new CombineSignal2[A, B, C](
      parent1 = this,
      parent2 = otherSignal,
      combinator = combinator
    )
  }

  def combine[B](otherSignal: Signal[B])(implicit composition: Composition[A, B]): Signal[composition.Composed] = {
    combineWith(otherSignal)(composition.compose)
  }

  def withCurrentValueOf[B](signal: Signal[B]): Signal[(A, B)] = {
    new SampleCombineSignal2[A, B, (A, B)](
      samplingSignal = this,
      sampledSignal1 = signal,
      combinator = Tuple2.apply[A, B]
    )
  }

  def sample[B](signal: Signal[B]): Signal[B] = {
    new SampleCombineSignal2[A, B, B](
      samplingSignal = this,
      sampledSignal1 = signal,
      combinator = (_, sampledValue) => sampledValue
    )
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

  /** Add a noop observer to this signal to ensure that it's started.
    * This lets you access .now and .tryNow on the resulting StrictSignal.
    *
    * You can use `myStream.toWeakSignal.observe` to read the last emitted
    * value from event streams just as well.
    */
  def observe(implicit owner: Owner): SignalViewer[A] = new SignalViewer(this, owner)

  /** Initial value is only evaluated if/when needed (when there are observers) */
  protected[airstream] def tryNow(): Try[A] = {
    maybeLastSeenCurrentValue.getOrElse {
      val currentValue = initialValue
      setCurrentValue(currentValue)
      currentValue
    }
  }

  /** See comment for [[tryNow]] right above
    *
    * @throws Exception if current value is an error
    */
  protected[airstream] def now(): A = tryNow().get

  protected[this] def setCurrentValue(newValue: Try[A]): Unit = {
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

  // @TODO[API] Use pattern match instead when isInstanceOf performance is fixed: https://github.com/scala-js/scala-js/issues/2066
  override protected def onAddedExternalObserver(observer: Observer[A]): Unit = {
    super.onAddedExternalObserver(observer)
    observer.onTry(tryNow()) // send current value immediately
  }

  override protected[this] final def fireValue(nextValue: A, transaction: Transaction): Unit = {
    fireTry(Success(nextValue), transaction)
  }

  override protected[this] final def fireError(nextError: Throwable, transaction: Transaction): Unit = {
    fireTry(Failure(nextError), transaction)
  }

  /** Signal propagates only if its value has changed */
  override protected[this] def fireTry(nextValue: Try[A], transaction: Transaction): Unit = {
    // @TODO[API] It is rather curious/unintuitive that firing external observers first seems to make more sense. Think about it some more.
    // @TODO[Performance] This might be suboptimal for some data structures (e.g. big maps). Document this along with workarounds.
    // Note: This comparison is using the Scala `equals` method. Typically `equals` calls `eq` first,
    // to check for reference equality, then proceeds to check for structural equality if needed.
    if (tryNow() != nextValue) {
      setCurrentValue(nextValue)

      // === CAUTION ===
      // The following logic must match EventStream's fireValue / fireError! It is separated here for performance.

      val isError = nextValue.isFailure
      var errorReported = false

      externalObservers.foreach { observer =>
        observer.onTry(nextValue)
        if (isError && !errorReported) errorReported = true
      }

      internalObservers.foreach { observer =>
        observer.onTry(nextValue, transaction)
        if (isError && !errorReported) errorReported = true
      }

      // This will only ever happen for special Signals that maintain their current value even without observers.
      // Currently we only have one kind of such signal: StrictSignal.
      //
      // We want to report unhandled errors on such signals if they have no observers (including internal observers)
      // because if we don't, the error will not be reported anywhere, and I think we would usually want it.
      if (isError && !errorReported) {
        nextValue.fold(AirstreamError.sendUnhandledError, _ => ())
      }
    }
  }
}

object Signal extends StaticSignalCombineMethods {

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
        onStart = start(setValue, getValue, getStartIndex, getIsStarted),
        onStop = stop(getStartIndex())
      )
    })
  }

  def combine[T1, T2](
    signal1: Signal[T1],
    signal2: Signal[T2]
  ): Signal[(T1, T2)] = {
    combineWith(signal1, signal2)(Tuple2.apply[T1, T2])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, Out](
    signal1: Signal[T1],
    signal2: Signal[T2]
  )(
    combinator: (T1, T2) => Out
  ): Signal[Out] = {
    new CombineSignal2(signal1, signal2, combinator)
  }

  def combineSeq[A](
    signals: Seq[Signal[A]]
  ): Signal[Seq[A]] = {
    new CombineSignalN[A, Seq[A]](signals, identity)
  }

  implicit def toCombinableSignal[A](signal: Signal[A]): CombinableSignal[A] = {
    new CombinableSignal(signal)
  }

  implicit def toTuple2Signal[A, B](signal: Signal[(A, B)]): Tuple2Signal[A, B] = {
    new Tuple2Signal(signal)
  }

  implicit def toSplittableSignal[M[_], Input](signal: Signal[M[Input]]): SplittableSignal[M, Input] = {
    new SplittableSignal(signal)
  }

  implicit def toSplittableOneSignal[A](signal: Signal[A]): SplittableOneSignal[A] = {
    new SplittableOneSignal[A](signal)
  }
}
