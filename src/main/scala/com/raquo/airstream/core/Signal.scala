package com.raquo.airstream.core

import com.raquo.airstream.combine.CombineSignalN
import com.raquo.airstream.combine.generated.{
  CombinableSignal,
  StaticSignalCombineOps
}
import com.raquo.airstream.core.Source.SignalSource
import com.raquo.airstream.custom.CustomSource._
import com.raquo.airstream.custom.{CustomSignalSource, CustomSource}
import com.raquo.airstream.debug.{DebuggableSignal, Debugger, DebuggerSignal}
import com.raquo.airstream.distinct.DistinctSignal
import com.raquo.airstream.extensions._
import com.raquo.airstream.misc.{MapSignal, ScanLeftSignal, StreamFromSignal}
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.split.{SplittableOneSignal, SplittableSignal}
import com.raquo.airstream.state.{ObservedSignal, OwnedSignal, Val}
import com.raquo.airstream.status.Status
import com.raquo.airstream.timing.JsPromiseSignal
import com.raquo.ew.JsArray

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.{Failure, Try}

/** Signal is an Observable with a current value. */
trait Signal[+A]
    extends Observable[A]
    with BaseObservable[Signal, A]
    with SignalSource[A] {

  protected[this] var _lastUpdateId: Int = 0

  protected[airstream] def lastUpdateId: Int = _lastUpdateId

  /** Get the signal's current value */
  protected[airstream] def tryNow(): Try[A]

  /** Get the signal's current value
    *
    * @throws Throwable
    *   the error from the current value's Failure
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

  /** Modify the Signal's changes stream, e.g. signal.composeChanges(_.delay(ms
    * \= 100))
    *
    * Alias to changes(operator). See also: [[composeAll]]
    *
    * @param operator
    *   Note: Must not throw!
    */
  def composeChanges[AA >: A](
      operator: EventStream[A] => EventStream[AA]
  ): Signal[AA] = {
    composeAll(changesOperator = operator, initialOperator = identity)
  }

  /** Modify both the Signal's changes stream, and its initial. Similar to
    * composeChanges, but lets you output a type unrelated to A.
    *
    * @param changesOperator
    *   Note: Must not throw!
    * @param initialOperator
    *   Note: Must not throw!
    */
  def composeAll[B](
      changesOperator: EventStream[A] => EventStream[B],
      initialOperator: Try[A] => Try[B],
      cacheInitialValue: Boolean = false
  ): Signal[B] = {
    changesOperator(changes).toSignalWithTry(
      initialOperator(tryNow()),
      cacheInitialValue
    )
  }

  // #TODO[API] Why is .changes a def, and not a lazy val?
  //  See `signal.changes shouldNotBe signal.changes` in SignalSpec
  /** A stream of all values in this signal, excluding the initial value.
    *
    * When re-starting this stream, it emits the signal's new current value if
    * and only if something has caused the signal's value to be updated or
    * re-evaluated while the changes stream was stopped. This way the changes
    * stream stays in sync with the signal even after restarting.
    */
  def changes: EventStream[A] =
    new StreamFromSignal[A](parent = this, changesOnly = true)

  /** Modify the Signal's changes, e.g. signal.changes(_.delay(ms = 100))
    *
    * Alias to [[composeChanges]]. See also: [[composeAll]]
    *
    * @param compose
    *   Note: Must not throw!
    */
  @inline def changes[AA >: A](
      compose: EventStream[A] => EventStream[AA]
  ): Signal[AA] = {
    composeChanges(compose)
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](makeInitial: A => B)(fn: (B, A) => B): Signal[B] =
    scanLeft(makeInitial)(fn)

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](makeInitial: Try[A] => Try[B])(
      fn: (Try[B], Try[A]) => Try[B]
  ): Signal[B] = scanLeftRecover(makeInitial)(fn)

  /** A signal that emits the accumulated value every time that the parent
    * signal emits.
    *
    * @param makeInitial
    *   Note: guarded against exceptions
    * @param fn
    *   Note: guarded against exceptions
    */
  def scanLeft[B](makeInitial: A => B)(fn: (B, A) => B): Signal[B] = {
    scanLeftRecover(parentInitial => parentInitial.map(makeInitial)) {
      (currentValue, nextParentValue) =>
        Try(fn(currentValue.get, nextParentValue.get))
    }
  }

  /** A signal that emits the accumulated value every time that the parent
    * signal emits.
    *
    * @param makeInitial
    *   currentParentValue => initialValue Note: must not throw
    * @param fn
    *   (currentValue, nextParentValue) => nextValue
    * @return
    */
  def scanLeftRecover[B](
      makeInitial: Try[A] => Try[B]
  )(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    new ScanLeftSignal(
      parent = this,
      makeInitialValue = () => makeInitial(tryNow()),
      fn
    )
  }

  /** Distinct all values (both events and errors) using a comparison function
    */
  override def distinctTry(isSame: (Try[A], Try[A]) => Boolean): Signal[A] = {
    new DistinctSignal[A](parent = this, isSame, resetOnStop = false)
  }

  /** @param pf Note: guarded against exceptions */
  override def recover[B >: A](
      pf: PartialFunction[Throwable, Option[B]]
  ): Signal[B] = {
    new MapSignal[A, B](
      this,
      project = identity,
      recover = Some(pf)
    )
  }

  override def recoverToTry: Signal[Try[A]] = {
    map(Try(_)).recover[Try[A]](err => Some(Failure(err)))
  }

  override def recoverToEither: Signal[Either[Throwable, A]] = {
    map(Right(_)).recover(err => Some(Left(err)))
  }

  /** See also debug methods in
    * [[com.raquo.airstream.debug.DebuggableObservable]]
    */
  override def debugWith(debugger: Debugger[A]): Signal[A] = {
    new DebuggerSignal[A](this, debugger)
  }

  /** Add a noop observer to this signal to ensure that it's started. This lets
    * you access .now and .tryNow on the resulting StrictSignal.
    *
    * You can use `myStream.toWeakSignal.observe.tryNow()` to read the last
    * emitted value from event streams just as well.
    */
  def observe(implicit owner: Owner): OwnedSignal[A] =
    new ObservedSignal(this, Observer.empty, owner)

  override def toObservable: Signal[A] = this

  /** Here we need to ensure that Signal's default value has been evaluated at
    * least once. It is important because if a Signal gets started by means of
    * its .changes stream acquiring an observer, nothing else would trigger this
    * evaluation because initialValue is not directly used by the .changes
    * stream. However, when the events start coming in, we will need this
    * initialValue because Signal needs to know when its current value has
    * changed.
    */
  override protected[this] def onStart(): Unit = {
    // println(s"$this onStart")
    tryNow() // trigger setCurrentValue if we didn't initialize this before
    super.onStart()
  }

  /** Recalculate the signal's current value. Typically this asks the parent
    * signal for its current value, and does the math from there, according to
    * the particular signal's logic (e.g. MapSignal would apply the `project`
    * function).
    *
    * This method is used to calculate the signal's initial value, but also to
    * recalculate, this signal's value, to re-sync it with the parent, when this
    * signal is restarted after being stopped. See
    * https://github.com/raquo/Airstream/issues/43
    */
  protected def currentValueFromParent(): Try[A]

  // @TODO[API] Use pattern match instead when isInstanceOf performance is fixed: https://github.com/scala-js/scala-js/issues/3815
  override protected def onAddedExternalObserver(
      observer: Observer[A]
  ): Unit = {
    observer.onTry(tryNow()) // send current value immediately
  }

}

object Signal {

  def fromValue[A](value: A): Val[A] = Val(value)

  def fromTry[A](value: Try[A]): Val[A] = Val.fromTry(value)

  def fromEither[A](value: Either[Throwable, A]): Val[A] = Val.fromEither(value)

  /** The signal will start with `None`, even if the future is already resolved.
    * Once the future resolves (or after a minimal async delay if it's already
    * resolved), the signal's value will be updated to the future's resolved
    * value.
    */
  def fromFuture[A](
      future: Future[A]
  )(implicit ec: ExecutionContext): Signal[Option[A]] = {
    fromJsPromise(future.toJSPromise(ec))
  }

  /** The signal will start with the provided `initial` value, even if the
    * future is already resolved. Once the future resolves (or after a minimal
    * async delay if it's already resolved), the signal's value will be updated
    * to the future's resolved value.
    */
  def fromFuture[A](future: Future[A], initial: => A)(implicit
      ec: ExecutionContext
  ): Signal[A] = {
    fromJsPromise(future.toJSPromise(ec), initial)
  }

  /** The signal will start with `None`, even if the promise is already
    * resolved. Once the promise resolves (or after a minimal async delay if
    * it's already resolved), the signal's value will be updated to the
    * promise's resolved value.
    */
  def fromJsPromise[A](promise: js.Promise[A]): Signal[Option[A]] = {
    new JsPromiseSignal(promise)
  }

  /** The signal will start with the provided `initial` value, even if the
    * promise is already resolved. Once the promise resolves (or after a minimal
    * async delay if it's already resolved), the signal's value will be updated
    * to the promise's resolved value.
    */
  def fromJsPromise[A](promise: js.Promise[A], initial: => A): Signal[A] = {
    new JsPromiseSignal(promise).map {
      case None        => initial
      case Some(value) => value
    }
  }

  // TODO[API] do we need this?
  /** A stream from js.Promise that kind-of sort-of behaves like a signal:
    *   - It only every emits once, when the promise resolves.
    *   - If you miss that event because this stream got stopped before the
    *     promise resolved, you will receive that event when you start this
    *     stream again
    *   - However, if this stream was not stopped, new subscribers will not
    *     receive the event. If you need such behaviour, use a proper signal
    *     instead.
    */
  // def fromJsPromiseToStream[A](promise: js.Promise[A]): EventStream[A] = {
  //   new JsPromiseSignal(promise).changes.map(_.get)
  // }

  /** Easy helper for custom signals. See [[CustomSignalSource]] for docs.
    *
    * Provide `start` and `stop` callbacks that will be called when the signal
    * is started and stopped. E.g. create some resource on start, clean it on
    * stop.
    *
    * The arguments to `start` are functions. Call them to do things like update
    * the signal's value, read its current value, or get some info:
    *
    * `getStartIndex` returns `1` the first time the signal is started, and is
    * incremented every time it is started again after being stopped.
    *
    * `getIsStarted` is a function that you can call any time, including after
    * some delay, to check if the signal is still started, e.g. if you don't
    * want to update the signal's value if the signal is stopped.
    *
    * @param stop
    *   MUST NOT THROW!
    */
  def fromCustomSource[A](
      initial: => Try[A],
      start: (
          SetCurrentValue[A],
          GetCurrentValue[A],
          GetStartIndex,
          GetIsStarted
      ) => Unit,
      stop: StartIndex => Unit
  ): Signal[A] = {
    new CustomSignalSource[A](
      getInitialValue = initial,
      makeConfig = (setValue, getValue, getStartIndex, getIsStarted) =>
        CustomSource.Config(
          onStart =
            () => start(setValue, getValue, getStartIndex, getIsStarted),
          onStop = () => stop(getStartIndex())
        )
    )
  }

  def sequence[A](
      signals: Seq[Signal[A]]
  ): Signal[Seq[A]] = {
    new CombineSignalN[A, Seq[A]](JsArray(signals: _*), _.asScalaJs.toSeq)
  }

  @inline def combineSeq[A](signals: Seq[Signal[A]]): Signal[Seq[A]] = sequence(
    signals
  )

  /** Provides methods on Signal companion object: combine, combineWith */
  implicit def toSignalCompanionCombineSyntax(
      @unused s: Signal.type
  ): StaticSignalCombineOps.type = StaticSignalCombineOps

  /** Provides methods on Signal: combine, combineWith, withCurrentValueOf,
    * sample
    */
  implicit def toCombinableSignal[A](signal: Signal[A]): CombinableSignal[A] =
    new CombinableSignal(signal)

  /** Provides methods on Signal: split, splitByIndex */
  implicit def toSplittableSignal[M[_], Input](
      signal: Signal[M[Input]]
  ): SplittableSignal[M, Input] = new SplittableSignal(signal)

  /** Provides methods on Signal: splitOne */
  implicit def toSplittableOneSignal[A](
      signal: Signal[A]
  ): SplittableOneSignal[A] = new SplittableOneSignal[A](signal)

  /** Provides methods on Signal: splitBoolean */
  implicit def toBooleanSignal(signal: Signal[Boolean]): BooleanSignal =
    new BooleanSignal(signal)

  /** Provides methods on Signal: splitOption */
  implicit def toOptionSignal[A](signal: Signal[Option[A]]): OptionSignal[A] =
    new OptionSignal(signal)

  /** Provides methods on Signal: splitEither */
  implicit def toEitherSignal[A, B](
      signal: Signal[Either[A, B]]
  ): EitherSignal[A, B] = new EitherSignal(signal)

  /** Provides methods on Signal: splitTry */
  implicit def toTrySignal[A](signal: Signal[Try[A]]): TrySignal[A] =
    new TrySignal(signal)

  /** Provides methods on Signal: splitStatus */
  implicit def toStatusSignal[In, Out](
      signal: Signal[Status[In, Out]]
  ): StatusSignal[In, Out] = new StatusSignal(signal)

  /** Provides signal-specific debug* methods: debugSpyInitialEval,
    * debugLogInitialEval, debugBreakInitialEval
    */
  implicit def toDebuggableSignal[A](signal: Signal[A]): DebuggableSignal[A] =
    new DebuggableSignal[A](signal)

  // toTupleSignalN conversions provide mapN method on Signals of tuples

  implicit def toTupleSignal2[T1, T2](
      stream: Signal[(T1, T2)]
  ): TupleSignal2[T1, T2] = new TupleSignal2(stream)

  implicit def toTupleSignal3[T1, T2, T3](
      stream: Signal[(T1, T2, T3)]
  ): TupleSignal3[T1, T2, T3] = new TupleSignal3(stream)

  implicit def toTupleSignal4[T1, T2, T3, T4](
      stream: Signal[(T1, T2, T3, T4)]
  ): TupleSignal4[T1, T2, T3, T4] = new TupleSignal4(stream)

  implicit def toTupleSignal5[T1, T2, T3, T4, T5](
      stream: Signal[(T1, T2, T3, T4, T5)]
  ): TupleSignal5[T1, T2, T3, T4, T5] = new TupleSignal5(stream)

  implicit def toTupleSignal6[T1, T2, T3, T4, T5, T6](
      stream: Signal[(T1, T2, T3, T4, T5, T6)]
  ): TupleSignal6[T1, T2, T3, T4, T5, T6] = new TupleSignal6(stream)

  implicit def toTupleSignal7[T1, T2, T3, T4, T5, T6, T7](
      stream: Signal[(T1, T2, T3, T4, T5, T6, T7)]
  ): TupleSignal7[T1, T2, T3, T4, T5, T6, T7] = new TupleSignal7(stream)

  implicit def toTupleSignal8[T1, T2, T3, T4, T5, T6, T7, T8](
      stream: Signal[(T1, T2, T3, T4, T5, T6, T7, T8)]
  ): TupleSignal8[T1, T2, T3, T4, T5, T6, T7, T8] = new TupleSignal8(stream)

  implicit def toTupleSignal9[T1, T2, T3, T4, T5, T6, T7, T8, T9](
      stream: Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]
  ): TupleSignal9[T1, T2, T3, T4, T5, T6, T7, T8, T9] = new TupleSignal9(stream)

  private var lastUpdateId: Int = 0

  def nextUpdateId(): Int = {
    // Note: Int.MaxValue is lower than JS native Number.MAX_SAFE_INTEGER
    if (lastUpdateId == Int.MaxValue) {
      // start with 1 because 0 is a special value indicating "initial value".
      lastUpdateId = 1
    } else {
      lastUpdateId += 1
    }
    lastUpdateId
  }
}
