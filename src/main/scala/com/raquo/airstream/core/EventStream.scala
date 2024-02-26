package com.raquo.airstream.core

import com.raquo.airstream.combine.generated._
import com.raquo.airstream.combine.{CombineStreamN, MergeStream}
import com.raquo.airstream.core.Source.{EventSource, SignalSource}
import com.raquo.airstream.custom.CustomSource._
import com.raquo.airstream.custom.{CustomSource, CustomStreamSource}
import com.raquo.airstream.debug.{DebuggableStream, Debugger, DebuggerStream}
import com.raquo.airstream.distinct.DistinctStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.extensions._
import com.raquo.airstream.javaflow.FlowPublisherStream
import com.raquo.airstream.misc._
import com.raquo.airstream.split.{SplittableOneStream, SplittableStream}
import com.raquo.airstream.status.{AsyncStatusObservable, Status}
import com.raquo.airstream.timing._
import com.raquo.ew.JsArray

import java.util.concurrent.Flow
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.{Failure, Success, Try}

trait EventStream[+A] extends Observable[A] with BaseObservable[EventStream, A] with EventSource[A] {

  override def map[B](project: A => B): EventStream[B] = {
    new MapStream(this, project, recover = None)
  }

  /** @param passes Note: guarded against exceptions */
  def filter(passes: A => Boolean): EventStream[A] = {
    new FilterStream(parent = this, passes)
  }

  def filterNot(predicate: A => Boolean): EventStream[A] = filter(!predicate(_))

  /** Filter stream events by a signal or var of boolean.
    *
    * `stream.filterWith(otherSignal, passes = _ == false)` is essentially like
    * `stream.filter(_ => otherSignal.now() == false)` (but it compiles)
    */
  def filterWith[B](source: SignalSource[B], passes: B => Boolean): EventStream[A] = {
    this
      .withCurrentValueOf(source)
      .collect { case (ev, sourceValue) if passes(sourceValue) => ev }
  }

  /** Filter stream events by a signal or var of boolean (passes when true). */
  def filterWith(source: SignalSource[Boolean]): EventStream[A] = {
    filterWith[Boolean](source, passes = _ == true)
  }

  /** Apply `pf` to each event and emit the resulting value,
    * or emit nothing if `pf` is not defined for that event.
    *
    * @param pf Note: guarded against exceptions
    */
  def collect[B](pf: PartialFunction[A, B]): EventStream[B] = collectOpt(pf.lift)

  /** Apply `fn` to parent stream event, and emit resulting x if it returns Some(x)
    *
    * @param fn Note: guarded against exceptions
    */
  def collectOpt[B](fn: A => Option[B]): EventStream[B] = {
    new CollectStream(parent = this, fn)
  }

  /** @param ms milliseconds of delay */
  def delay(ms: Int = 0): EventStream[A] = {
    new DelayStream(parent = this, ms)
  }

  /** Based on [[delay]], but tracks the status of input and output to that operator. See [[Status]]. */
  def delayWithStatus(ms: Int): EventStream[Status[A, A]] = {
    AsyncStatusObservable[A, A, EventStream](this, _.delay(ms))
  }

  /** Make a stream that emits this stream's values but waits for `after` stream to emit first in a given transaction.
    * You can use this for Signals too with `Signal.composeChanges` (see docs for more details)
    */
  def delaySync(after: EventStream[_]): EventStream[A] = {
    new SyncDelayStream[A](parent = this, after = after)
  }

  /** See docs for [[ThrottleStream]] */
  def throttle(ms: Int, leading: Boolean = true): EventStream[A] = {
    new ThrottleStream(parent = this, intervalMs = ms, leading = leading)
  }

  /** Based on [[throttle]], but tracks the status of input and output to that operator. See [[Status]]. */
  def throttleWithStatus(ms: Int, leading: Boolean = true): EventStream[Status[A, A]] = {
    AsyncStatusObservable[A, A, EventStream](this, _.throttle(ms, leading))
  }

  /** See docs for [[DebounceStream]] */
  def debounce(ms: Int): EventStream[A] = {
    new DebounceStream(parent = this, ms)
  }

  /** Based on [[debounce]], but tracks the status of input and output to that operator. See [[Status]]. */
  def debounceWithStatus(ms: Int): EventStream[Status[A, A]] = {
    AsyncStatusObservable[A, A, EventStream](this, _.debounce(ms))
  }

  /** Drop (skip) the first `numEvents` events from this stream. Note: errors are NOT dropped.
    *
    * @param resetOnStop  Reset the count if the stream stops
    */
  def drop(numEvents: Int, resetOnStop: Boolean = false): EventStream[A] = {
    var numDropped = 0
    new DropStream[A](
      parent = this,
      dropWhile = _ => {
        val shouldDrop = numDropped < numEvents
        if (shouldDrop) {
          numDropped += 1
        }
        shouldDrop
      },
      reset = () => {
        numDropped = 0
      },
      resetOnStop
    )
  }

  /** Drop (skip) events from this stream as long as they pass the test (as soon as they stop passing, stop dropping)
    * Note: errors are NOT dropped.
    *
    * @param passes       Note: MUST NOT THROW!
    * @param resetOnStop  Forget everything and start dropping again if the stream stops
    */
  def dropWhile(passes: A => Boolean, resetOnStop: Boolean = false): EventStream[A] = {
    new DropStream[A](
      parent = this,
      dropWhile = ev => passes(ev),
      reset = () => (),
      resetOnStop
    )
  }

  /** Drop (skip) events from this stream as long as they do NOT pass the test (as soon as they start passing, stop dropping)
    * Note: errors are NOT dropped.
    *
    * @param passes       Note: MUST NOT THROW!
    * @param resetOnStop  Forget everything and start dropping again if the stream stops
    */
  def dropUntil(passes: A => Boolean, resetOnStop: Boolean = false): EventStream[A] = {
    new DropStream[A](
      parent = this,
      dropWhile = ev => !passes(ev),
      () => (),
      resetOnStop
    )
  }

  /** Take the first `numEvents` events from this stream, ignore the rest.
    * Note: As long as events are being taken, ALL errors are also taken
    *
    * @param resetOnStop  Reset the count if the stream stops
    */
  def take(numEvents: Int, resetOnStop: Boolean = false): EventStream[A] = {
    var numTaken = 0
    new TakeStream[A](
      parent = this,
      takeWhile = _ => {
        val shouldTake = numTaken < numEvents
        if (shouldTake) {
          numTaken += 1
        }
        shouldTake
      },
      reset = () => {
        numTaken = 0
      },
      resetOnStop
    )
  }

  /** Imitate parent stream as long as events pass the test; stop emitting after that.
    *
    * @param passes       Note: MUST NOT THROW!
    * @param resetOnStop  Forget everything and start dropping again if the stream stops
    */
  def takeWhile(passes: A => Boolean, resetOnStop: Boolean = false): EventStream[A] = {
    new TakeStream[A](
      parent = this,
      takeWhile = ev => passes(ev),
      reset = () => (),
      resetOnStop
    )
  }

  /** Imitate parent stream as long as events to NOT pass the test; stop emitting after that.
    *
    * @param passes       Note: MUST NOT THROW!
    * @param resetOnStop  Forget everything and start dropping again if the stream stops
    */
  def takeUntil(passes: A => Boolean, resetOnStop: Boolean = false): EventStream[A] = {
    new TakeStream[A](
      parent = this,
      takeWhile = ev => !passes(ev),
      () => (),
      resetOnStop
    )
  }

  /** Returns a stream that emits events from this stream AND all off the `streams`, interleaved.
    * Note: For other types of combination, see `combineWith`, `withCurrentValueOf`, `sample` etc.
    */
  def mergeWith[B >: A](streams: EventStream[B]*): EventStream[B] = {
    val allStreams = this +: streams
    EventStream.merge(allStreams: _*)
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](initial: B)(fn: (B, A) => B): Signal[B] = scanLeft(initial)(fn)

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = scanLeftRecover(initial)(fn)

  // @TODO[API] Should we introduce some kind of FoldError() wrapper?
  /** A signal that emits the accumulated value every time that the parent stream emits.
    *
    * See also: [[startWith]]
    *
    * @param fn Note: guarded against exceptions
    */
  def scanLeft[B](initial: B)(fn: (B, A) => B): Signal[B] = {
    scanLeftRecover(Success(initial)) { (currentValue, nextParentValue) =>
      Try(fn(currentValue.get, nextParentValue.get))
    }
  }

  /** A signal that emits the accumulated value every time that the parent stream emits.
    *
    * @param fn Note: Must not throw!
    */
  def scanLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = {
    new ScanLeftSignal(parent = this, () => initial, fn)
  }

  /** Convert stream to signal, given an initial value
    *
    * @param cacheInitialValue if false, signal's initial value will be re-evaluated on every
    *                          restart (so long as the parent stream does not emit any values)
    */
  @inline def startWith[B >: A](initial: => B, cacheInitialValue: Boolean = false): Signal[B] = {
    toSignal(initial, cacheInitialValue)
  }

  /** @param cacheInitialValue if false, signal's initial value will be re-evaluated on every
    *                          restart (so long as the parent stream does not emit any values)
    */
  @inline def startWithTry[B >: A](initial: => Try[B], cacheInitialValue: Boolean = false): Signal[B] = {
    toSignalWithTry(initial, cacheInitialValue)
  }

  @inline def startWithNone: Signal[Option[A]] = toWeakSignal

  /** @param cacheInitialValue if false, signal's initial value will be re-evaluated on every
    *                          restart (so long as the parent stream does not emit any values)
    */
  def toSignal[B >: A](initial: => B, cacheInitialValue: Boolean = false): Signal[B] = {
    toSignalWithTry(Success(initial), cacheInitialValue)
  }

  /** @param cacheInitialValue if false, signal's initial value will be re-evaluated on every
    *                          restart (so long as the parent stream does not emit any values)
    */
  def toSignalWithTry[B >: A](initial: => Try[B], cacheInitialValue: Boolean = false): Signal[B] = {
    new SignalFromStream(this, initial, cacheInitialValue)
  }

  /** @param cacheInitialValue if false, signal's initial value will be re-evaluated on every
    *                          restart (so long as the parent stream does not emit any values)
    */
  def toSignalWithEither[B >: A](initial: => Either[Throwable, B], cacheInitialValue: Boolean = false): Signal[B] = {
    new SignalFromStream(this, initial.toTry, cacheInitialValue)
  }

  /** Just a convenience helper. stream.compose(f) is equivalent to f(stream) */
  def compose[B](operator: EventStream[A] => EventStream[B]): EventStream[B] = {
    operator(this)
  }

  /** Distinct all values (both events and errors) using a comparison function */
  override def distinctTry(isSame: (Try[A], Try[A]) => Boolean): EventStream[A] = {
    new DistinctStream[A](parent = this, isSame, resetOnStop = false)
  }

  /** See docs for [[MapStream]]
    *
    * @param pf Note: guarded against exceptions
    */
  override def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): EventStream[B] = {
    new MapStream[A, B](
      parent = this,
      project = identity,
      recover = Some(pf)
    )
  }

  override def recoverToTry: EventStream[Try[A]] = {
    map(Try(_)).recover { case err => Some(Failure(err)) }
  }

  override def recoverToEither: EventStream[Either[Throwable, A]] = {
    map(Right(_)).recover { case err => Some(Left(err)) }
  }

  /** See also various debug methods in [[com.raquo.airstream.debug.DebuggableObservable]] */
  override def debugWith(debugger: Debugger[A]): EventStream[A] = {
    new DebuggerStream[A](this, debugger)
  }

  /** This is used in Signal-s. It's a no-op for Streams. */
  override protected def onAddedExternalObserver(observer: Observer[A]): Unit = ()

  override def toObservable: EventStream[A] = this

}

object EventStream {

  /** Event stream that never emits anything */
  val empty: EventStream[Nothing] = {
    fromCustomSource[Nothing](
      shouldStart = _ => false,
      start = (_, _, _, _) => (),
      stop = _ => ()
    )
  }

  def unit(emitOnce: Boolean = false): EventStream[Unit] = {
    EventStream.fromValue((), emitOnce)
  }

  /** @param emitOnce if true, the event will be emitted at most one time.
    *                 If false, the event will be emitted every time the stream is started.
    */
  def fromSeq[A](events: Seq[A], emitOnce: Boolean = false): EventStream[A] = {
    fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, _, _, _) => events.foreach(fireEvent),
      stop = _ => ()
    )
  }

  /** @param emitOnce if true, the event will be emitted at most one time.
    *                 If false, the event will be emitted every time the stream is started.
    */
  def fromValue[A](event: A, emitOnce: Boolean = false): EventStream[A] = {
    fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, _, _, _) => fireEvent(event),
      stop = _ => ()
    )
  }

  /** @param emitOnce if true, the event will be emitted at most one time.
    *                 If false, the event will be emitted every time the stream is started.
    */
  def fromTry[A](value: Try[A], emitOnce: Boolean = false): EventStream[A] = {
    fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, fireError, _, _) => value.fold(fireError, fireEvent),
      stop = _ => ()
    )
  }

  /** @param emitOnce if true, the event will be emitted at most one time.
    *                 If false, the event will be emitted every time the stream is started.
    */
  def fromEither[A](value: Either[Throwable, A], emitOnce: Boolean = false): EventStream[A] = {
    fromCustomSource[A](
      shouldStart = startIndex => if (emitOnce) startIndex == 1 else true,
      start = (fireEvent, fireError, _, _) => value.fold(fireError, fireEvent),
      stop = _ => ()
    )
  }

  def fromFuture[A](future: Future[A], emitOnce: Boolean = false)(implicit ec: ExecutionContext): EventStream[A] = {
    fromJsPromise(future.toJSPromise(ec), emitOnce)
  }

  def fromJsPromise[A](promise: js.Promise[A], emitOnce: Boolean = false): EventStream[A] = {
    new JsPromiseStream[A](promise, emitOnce)
  }

  /** Create a stream from a [[java.util.concurrent.Flow.Publisher]]
    * - Use this to bring in events from other streaming libraries
    * that can provide a `Flow.Publisher`, such as FS2 an Monix.
    */
  def fromPublisher[A](publisher: Flow.Publisher[A], emitOnce: Boolean = false): EventStream[A] = {
    FlowPublisherStream(publisher, emitOnce)
  }

  /** Easy helper for custom events. See [[CustomStreamSource]] for docs.
    *
    * Provide `start` and `stop` callbacks that will be called when the stream
    * is started and stopped. E.g. create some resource on start, clean it on stop.
    *
    * The arguments to `start` are functions. Call them to do things like emit an
    * event, emit an error, or get some info:
    *
    * `getStartIndex` returns `1` the first time the signal is started, and is
    * incremented every time it is started again after being stopped.
    *
    * `getIsStarted` is a function that you can call any time, including
    * after some delay, to check if the signal is still started, e.g. if
    * you don't want to update the signal's value if the signal is stopped.
    *
    * @param stop MUST NOT THROW!
    */
  def fromCustomSource[A](
    shouldStart: StartIndex => Boolean = _ => true,
    start: (FireValue[A], FireError, GetStartIndex, GetIsStarted) => Unit,
    stop: StartIndex => Unit
  ): EventStream[A] = {
    new CustomStreamSource[A](
      (fireValue, fireError, getStartIndex, getIsStarted) =>
      CustomSource.Config(
        onStart = () => start(fireValue, fireError, getStartIndex, getIsStarted),
        onStop = () => stop(getStartIndex())
      ).when {
        () => shouldStart(getStartIndex())
      }
    )
  }

  /** Create a stream and a callback that, when fired, makes that stream emit. */
  def withCallback[A]: (EventStream[A], A => Unit) = {
    val bus = new EventBus[A]
    (bus.events, bus.writer.onNext)
  }

  /** Create a stream of Unit, and a callback that, when fired, makes that stream emit. */
  def withUnitCallback: (EventStream[Unit], () => Unit) = {
    val bus = new EventBus[Unit]
    (bus.events, () => bus.writer.onNext(()))
  }

  /** Create a stream and a JS callback that, when fired, makes that stream emit. */
  def withJsCallback[A]: (EventStream[A], js.Function1[A, Unit]) = {
    val bus = new EventBus[A]
    (bus.events, bus.writer.onNext)
  }

  /** Create a stream and an observer that, when receiving an event or an error, makes that stream emit. */
  def withObserver[A]: (EventStream[A], Observer[A]) = {
    val bus = new EventBus[A]
    (bus.events, bus.writer)
  }

  /** Emit () with a delay (`ms` milliseconds after stream is started) */
  @inline def delay(ms: Int): EventStream[Unit] = delay(ms, ())

  /** Emit `event` with a delay (`ms` milliseconds after stream is started) */
  def delay[A](ms: Int, event: A, emitOnce: Boolean = false): EventStream[A] = {
    EventStream.fromValue(event, emitOnce).delay(ms)
  }

  def periodic(
    intervalMs: Int,
    resetOnStop: Boolean = false
  ): PeriodicStream[Int] = {
    new PeriodicStream[Int](
      initial = 0,
      next = eventNumber => Some((eventNumber + 1, intervalMs)),
      resetOnStop = resetOnStop
    )
  }

  def sequence[A](streams: Seq[EventStream[A]]): EventStream[Seq[A]] = {
    new CombineStreamN[A, Seq[A]](JsArray(streams: _*), _.asScalaJs.toSeq)
  }

  @inline def combineSeq[A](streams: Seq[EventStream[A]]): EventStream[Seq[A]] = sequence(streams)

  /** Returns a stream that emits events from all off the `streams`, interleaved. */
  def merge[A](streams: EventStream[A]*): EventStream[A] = {
    new MergeStream[A](JsArray(streams: _*))
  }

  def mergeSeq[A](streams: Seq[EventStream[A]]): EventStream[A] = {
    new MergeStream[A](JsArray(streams: _*))
  }

  /** Provides methods on EventStream companion object: combine, combineWith */
  implicit def toEventStreamCompanionCombineSyntax(@unused s: EventStream.type): StaticStreamCombineOps.type = StaticStreamCombineOps

  /** Provides methods on EventStream: combine, combineWith, withCurrentValueOf, sample */
  implicit def toCombinableStream[A](stream: EventStream[A]): CombinableStream[A] = new CombinableStream(stream)

  /** Provides methods on EventStream: split, splitByIndex */
  implicit def toSplittableStream[M[_], Input](stream: EventStream[M[Input]]): SplittableStream[M, Input] = new SplittableStream(stream)

  /** Provides methods on EventStream: splitOne */
  implicit def toSplittableOneStream[A](stream: EventStream[A]): SplittableOneStream[A] = new SplittableOneStream(stream)

  /** Provides methods on stream: splitBoolean */
  implicit def toBooleanStream[A](stream: EventStream[Boolean]): BooleanStream = new BooleanStream(stream)

  /** Provides methods on stream: filterSome, collectSome, splitOption */
  implicit def toOptionStream[A](stream: EventStream[Option[A]]): OptionStream[A] = new OptionStream(stream)

  /** Provides methods on stream: collectLeft, collectRight */
  implicit def toEitherStream[A, B](stream: EventStream[Either[A, B]]): EitherStream[A, B] = new EitherStream(stream)

  /** Provides methods on stream: collectFailure, collectSuccess */
  implicit def toTryStream[A](stream: EventStream[Try[A]]): TryStream[A] = new TryStream(stream)

  /** Provides methods on stream: collectOutput, collectResolved, collectPending, collectPendingInput, splitStatus */
  implicit def toStatusStream[In, Out](stream: EventStream[Status[In, Out]]): StatusStream[In, Out] = new StatusStream(stream)

  /** Provides debug* methods on EventStream: debugSpy, debugLogEvents, debugBreakErrors, etc. */
  implicit def toDebuggableStream[A](stream: EventStream[A]): DebuggableStream[A] = new DebuggableStream[A](stream)

  // toTupleStreamN conversions provide mapN and filterN methods on Signals of tuples

  implicit def toTupleStream2[T1, T2](stream: EventStream[(T1, T2)]): TupleStream2[T1, T2] = new TupleStream2(stream)

  implicit def toTupleStream3[T1, T2, T3](stream: EventStream[(T1, T2, T3)]): TupleStream3[T1, T2, T3] = new TupleStream3(stream)

  implicit def toTupleStream4[T1, T2, T3, T4](stream: EventStream[(T1, T2, T3, T4)]): TupleStream4[T1, T2, T3, T4] = new TupleStream4(stream)

  implicit def toTupleStream5[T1, T2, T3, T4, T5](stream: EventStream[(T1, T2, T3, T4, T5)]): TupleStream5[T1, T2, T3, T4, T5] = new TupleStream5(stream)

  implicit def toTupleStream6[T1, T2, T3, T4, T5, T6](stream: EventStream[(T1, T2, T3, T4, T5, T6)]): TupleStream6[T1, T2, T3, T4, T5, T6] = new TupleStream6(stream)

  implicit def toTupleStream7[T1, T2, T3, T4, T5, T6, T7](stream: EventStream[(T1, T2, T3, T4, T5, T6, T7)]): TupleStream7[T1, T2, T3, T4, T5, T6, T7] = new TupleStream7(stream)

  implicit def toTupleStream8[T1, T2, T3, T4, T5, T6, T7, T8](stream: EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)]): TupleStream8[T1, T2, T3, T4, T5, T6, T7, T8] = new TupleStream8(stream)

  implicit def toTupleStream9[T1, T2, T3, T4, T5, T6, T7, T8, T9](stream: EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]): TupleStream9[T1, T2, T3, T4, T5, T6, T7, T8, T9] = new TupleStream9(stream)
}
