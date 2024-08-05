package com.raquo.airstream.core

import com.raquo.airstream.debug.{
  DebuggableObservable,
  DebuggableSignal,
  Debugger
}
import com.raquo.airstream.flatten.{
  AllowFlatMap,
  FlattenStrategy,
  MergingStrategy,
  SwitchingStrategy
}
import com.raquo.airstream.ownership.{Owner, Subscription}
import com.raquo.airstream.status.{FlatMapStatusObservable, Status}
import com.raquo.ew.JsArray

import scala.annotation.unused
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** This trait represents a reactive value that can be subscribed to.
  *
  * It has only one direct subtype, [[Observable]], which in turn has two direct
  * subtypes, [[EventStream]] and [[Signal]].
  *
  * [[BaseObservable]] is the same as [[Observable]], it just lives in a
  * separate trait for technical reasons (the Self type param).
  *
  * All Observables are lazy. An Observable starts when it gets its first
  * observer (internal or external), and stops when it loses its last observer
  * (again, internal or external).
  *
  * Basic idea: Lazy Observable only holds references to those children that
  * have any observers (either directly on themselves, or on any of their
  * descendants). What this achieves:
  *   - Stream only propagates its value to children that (directly or not) have
  *     observers
  *   - Stream calculates its value only once regardless of how many observers /
  *     children it has) (so, all streams are "hot" observables)
  *   - Stream doesn't hold references to Streams that no one observes, allowing
  *     those Streams to be garbage collected if they are otherwise unreachable
  *     (which they should become when their subscriptions are killed by their
  *     owners)
  */
trait BaseObservable[+Self[+_] <: Observable[_], +A]
    extends Source[A]
    with Named {

  @inline protected implicit def protectedAccessEvidence: Protected =
    Protected.protectedAccessEvidence

  /** Note: Use Protected.topoRank(observable) to read another observable's
    * topoRank if needed
    */
  protected val topoRank: Int

  /** @param project Note: guarded against exceptions */
  def map[B](project: A => B): Self[B]

  /** `value` is passed by name, so it will be evaluated whenever the Observable
    * fires. Use it to sample mutable values (e.g. myInput.ref.value in
    * Laminar).
    *
    * See also: [[mapToStrict]]
    *
    * @param value
    *   Note: guarded against exceptions
    */
  def mapTo[B](value: => B): Self[B] = map(_ => value)

  /** `value` is evaluated strictly, only once, when this method is called.
    *
    * See also: [[mapTo]]
    */
  def mapToStrict[B](value: B): Self[B] = map(_ => value)

  def mapToUnit: Self[Unit] = map(_ => ())

  /** #WARNING: DO NOT USE THIS METHOD. See
    * https://github.com/raquo/Airstream/#flattening-observables
    */
  @inline def flatMap[B, Inner[_], Output[+_] <: Observable[_]](
      project: A => Inner[B]
  )(implicit
      strategy: SwitchingStrategy[Self, Inner, Output],
      @unused allowFlatMap: AllowFlatMap
  ): Output[B] = {
    strategy.flatten(map(project))
  }

  /** Alias to flatMapSwitch(_ => s) */
  @inline def flatMapTo[B, Inner[_], Output[+_] <: Observable[_]](
      s: => Inner[B]
  )(implicit
      strategy: SwitchingStrategy[Self, Inner, Output]
  ): Output[B] = {
    strategy.flatten(map(_ => s))
  }

  /** @param project Note: guarded against exceptions */
  @inline def flatMapSwitch[B, Inner[_], Output[+_] <: Observable[_]](
      project: A => Inner[B]
  )(implicit
      strategy: SwitchingStrategy[Self, Inner, Output]
  ): Output[B] = {
    strategy.flatten(map(project))
  }

  /** @param project Note: guarded against exceptions */
  @inline def flatMapMerge[B, Inner[_], Output[+_] <: Observable[_]](
      project: A => Inner[B]
  )(implicit
      strategy: MergingStrategy[Self, Inner, Output]
  ): Output[B] = {
    strategy.flatten(map(project))
  }

  /** @param project Note: guarded against exceptions */
  @inline def flatMapCustom[B, Inner[_], Output[+_] <: Observable[_]](
      project: A => Inner[B]
  )(
      strategy: FlattenStrategy[Self, Inner, Output]
  ): Output[B] = {
    strategy.flatten(map(project))
  }

  /** Based on [[flatMapSwitch]], but tracks the status of input and output to
    * flatMap. See [[Status]].
    */
  def flatMapWithStatus[B](project: A => EventStream[B]): Self[Status[A, B]] = {
    FlatMapStatusObservable[A, B, Self](this, project)
  }

  /** Shorthand for `flatMapWithStatus(_ => innerStream)`. */
  def flatMapWithStatus[B](
      innerStream: => EventStream[B]
  ): Self[Status[A, B]] = {
    flatMapWithStatus(_ => innerStream)
  }

  /** Distinct events (but keep all errors) by == (equals) comparison */
  def distinct: Self[A] = distinctByFn(_ == _)

  /** Distinct events (but keep all errors) by matching key Note: `key(event)`
    * might be evaluated more than once for each event
    */
  def distinctBy(key: A => Any): Self[A] = distinctByFn(key(_) == key(_))

  /** Distinct events (but keep all errors) by reference equality (eq) */
  def distinctByRef(implicit ev: A <:< AnyRef): Self[A] = distinctByFn(
    ev(_) eq ev(_)
  )

  /** Distinct events (but keep all errors) using a comparison function */
  def distinctByFn(isSame: (A, A) => Boolean): Self[A] = distinctTry {
    case (Success(prev), Success(next)) => isSame(prev, next)
    case _                              => false
  }

  /** Distinct errors only (but keep all events) using a comparison function */
  def distinctErrors(isSame: (Throwable, Throwable) => Boolean): Self[A] =
    distinctTry {
      case (Failure(prevErr), Failure(nextErr)) => isSame(prevErr, nextErr)
      case _                                    => false
    }

  /** Distinct all values (both events and errors) using a comparison function
    */
  def distinctTry(isSame: (Try[A], Try[A]) => Boolean): Self[A]

  /** Execute a side effecting callback every time the observable emits. If it's
    * a signal, it also runs when its initial value is evaluated.
    *
    * See https://github.com/raquo/Airstream/#tapEach for more details.
    */
  def tapEach[U](f: A => U): Self[A] = map { v =>
    f(v); v
  }

  def toStreamIfSignal[B >: A](
      ifSignal: Signal[A] => EventStream[B]
  ): EventStream[B] = {
    matchStreamOrSignal(identity, ifSignal)
  }

  def toSignalIfStream[B >: A](
      ifStream: EventStream[A] => Signal[B]
  ): Signal[B] = {
    matchStreamOrSignal(ifStream, identity)
  }

  /** Convert this observable to a signal of Option[A]. If it is a stream, set
    * initial value to None.
    */
  def toWeakSignal: Signal[Option[A]] = {
    matchStreamOrSignal(
      ifStream =
        _.map(Some(_)).toSignal(initial = None, cacheInitialValue = true),
      ifSignal = _.map(Some(_))
    )
  }

  def matchStreamOrSignal[B](
      ifStream: EventStream[A] => B,
      ifSignal: Signal[A] => B
  ): B = {
    this match {
      case stream: EventStream[A @unchecked] => ifStream(stream)
      case signal: Signal[A @unchecked]      => ifSignal(signal)
      case _ =>
        throw new Exception("All Observables must extend EventStream or Signal")
    }
  }

  // @TODO[API] I don't like the Option[O] output type here very much. We should consider a sentinel error object instead (need to check performance). Or maybe add a recoverOrSkip method or something?
  /** @param pf Note: guarded against exceptions */
  def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Self[B]

  def recoverIgnoreErrors: Self[A] = recover[A](_ => None)

  // #TODO[Scala] I'm not sure why I can't implement this method here. Getting weird type error about `Self`.
  /** Convert this to an observable that emits Failure(err) instead of erroring
    */
  def recoverToTry: Self[Try[A]]

  /** Convert this to an observable that emits Left(err) instead of erroring */
  def recoverToEither: Self[Either[Throwable, A]]

  /** Create a new observable that listens to this one and has a debugger
    * attached.
    *
    * Use the resulting observable in place of the original observable in your
    * code. See docs for details.
    *
    * There are more convenient methods available implicitly from
    * [[DebuggableObservable]] and [[DebuggableSignal]], such as debugLog(),
    * debugSpyEvents(), etc.
    */
  def debugWith(debugger: Debugger[A]): Self[A]

  /** Create an external observer from a function and subscribe it to this
    * observable.
    *
    * Note: since you won't have a reference to the observer, you will need to
    * call Subscription.kill() to unsubscribe
    */
  def foreach(onNext: A => Unit)(implicit owner: Owner): Subscription = {
    val observer = Observer(onNext)
    addObserver(observer)(owner)
  }

  /** Subscribe an external observer to this observable */
  def addObserver(observer: Observer[A])(implicit owner: Owner): Subscription

  protected[this] def addExternalObserver(
      observer: Observer[A],
      owner: Owner
  ): Subscription

  protected[this] def onAddedExternalObserver(observer: Observer[A]): Unit

  /** Child observable should call this method on its parents when it is
    * started. This observable calls [[onStart]] if this action has given it its
    * first observer (internal or external).
    *
    * I think `shouldCallMaybeWillStart` should be `false` when we're calling
    * this from onStart. In that case, `maybeWillStart` was already called.
    * #TODO But `maybeWillStart` checks for willStartDone status, so does it
    * actually matter?
    */
  protected[airstream] def addInternalObserver(
      observer: InternalObserver[A],
      shouldCallMaybeWillStart: Boolean
  ): Unit

  /** Note: do not expose this to end users. See
    * https://github.com/raquo/Airstream/issues/10
    *
    * Removal still happens synchronously, just after we're done iterating over
    * this observable's observers, to avoid interference with that logic.
    *
    * Note: The delay is necessary not just because of interference with actual
    * while(index < observers.length) iteration, but also because on a high
    * level it is too risky to remove observers from arbitrary observables while
    * the propagation is running. This would mean that some graphs would not
    * propagate fully, which would break very basic expectations of end users.
    *
    * UPDATE: In 15.0.0 we made the observable removal delay more fine grained –
    * previously we would wait until the whole transaction completed before
    * removing observers, now we only wait until this observable's iteration is
    * done. This helped us fix https://github.com/raquo/Airstream/issues/95
    *
    * Note: To completely unsubscribe an Observer from this Observable, you need
    * to remove it as many times as you added it to this Observable.
    */
  protected[airstream] def removeExternalObserver(
      observer: Observer[A]
  ): Unit = {
    if (isSafeToRemoveObserver) {
      // remove right now – useful for efficient recursive removals
      removeExternalObserverNow(observer)
    } else {
      // schedule removal to happen when it's safe
      getOrCreatePendingObserverRemovals.push(() =>
        removeExternalObserverNow(observer)
      )
    }
  }

  /** Safely remove internal observer (such that it doesn't interfere with
    * iteration over the list of observers). Removal still happens
    * synchronously, just after we're done iterating over this observable's
    * observers.
    *
    * Child observable should call this method on its parents when it is
    * stopped.
    */
  protected[airstream] def removeInternalObserver(
      observer: InternalObserver[A]
  ): Unit = {
    if (isSafeToRemoveObserver) {
      // remove right now – useful for efficient recursive removals
      removeInternalObserverNow(observer)
    } else {
      // schedule removal to happen when it's safe
      getOrCreatePendingObserverRemovals.push(() =>
        removeInternalObserverNow(observer)
      )
    }
  }

  /** Child observable should call removeInternalObserver(parent,
    * childInternalObserver) when it is stopped. This observable calls
    * [[onStop]] if this action has removed its last observer (internal or
    * external).
    */
  protected[airstream] def removeInternalObserverNow(
      observer: InternalObserver[A]
  ): Unit

  protected[airstream] def removeExternalObserverNow(
      observer: Observer[A]
  ): Unit

  /** Total number of internal and external observers */
  protected def numAllObservers: Int

  protected def isStarted: Boolean = numAllObservers > 0

  protected var isSafeToRemoveObserver: Boolean = true

  /** Observer removals scheduled to run as soon as this observable's event
    * propagation finishes. Only put calls to `removeInternalObserverNow` and
    * `removeExternalObserverNow` here, no custom logic.
    */
  protected var maybePendingObserverRemovals: js.UndefOr[JsArray[() => Unit]] =
    js.undefined

  protected def getOrCreatePendingObserverRemovals: JsArray[() => Unit] = {
    maybePendingObserverRemovals.getOrElse {
      val newArray = JsArray[() => Unit]()
      maybePendingObserverRemovals = newArray
      newArray
    }
  }

  /** When starting an observable, this is called recursively on every one of
    * its parents that are not started. This whole chain happens before onStart
    * callback is called. This chain serves to prepare the internal states of
    * observables that are about to start, e.g. you should update the signal's
    * value to match its parent signal's value in this callback, if applicable.
    *
    * Default implementation, for observables that don't need anything, should
    * be to call `parent.maybeWillStart()` for every parent observable.
    *
    * If custom behaviour is required, you should generally call
    * `parent.maybeWillStart()` BEFORE your custom logic. Then your logic will
    * be able to make use of parent's updated value.
    *
    * Note: NEVER CALL onWillStart() DIRECTLY Call maybeWillStart() instead, it
    * keeps track of important internal state.
    *
    * Note: THIS METHOD MUST NOT CREATE TRANSACTIONS OR FIRE ANY EVENTS! DO IT
    * IN ONSTART IF NEEDED.
    */
  protected def onWillStart(): Unit

  protected def maybeWillStart(): Unit

  /** This method is fired when this observable starts working (listening for
    * parent events and/or firing its own events), that is, when it gets its
    * first Observer (internal or external).
    *
    * [[onStart]] can potentially be called multiple times, the second time
    * being after it has stopped (see [[onStop]]).
    */
  protected def onStart(): Unit = ()

  /** This method is fired when this observable stops working (listening for
    * parent events and/or firing its own events), that is, when it loses its
    * last Observer (internal or external).
    *
    * [[onStop]] can potentially be called multiple times, the second time being
    * after it has started again (see [[onStart]]).
    */
  protected def onStop(): Unit = ()

  /** Airstream may internally use Scala library functions which use `==` or
    * `hashCode` for equality, for example List.contains. Comparing observables
    * by structural equality pretty much never makes sense, yet it's not that
    * hard to run into that, all you need is to create a `case class` subclass,
    * and the Scala compiler will generate a structural-equality `equals` and
    * `hashCode` methods for you behind the scenes.
    *
    * To prevent that, we make equals and hashCode methods final, using the
    * default implementation (which is reference equality).
    */
  final override def equals(obj: Any): Boolean = super.equals(obj)

  /** Force reference equality checks. See comment for `equals`. */
  final override def hashCode(): Int = super.hashCode()
}

object BaseObservable {

  @inline private[airstream] def topoRank[O[+_] <: Observable[_]](
      observable: BaseObservable[O, _]
  ): Int = {
    observable.topoRank
  }

  @inline private[airstream] def maybeWillStart[O[+_] <: Observable[_]](
      observable: BaseObservable[O, _]
  ): Unit = {
    observable.maybeWillStart()
  }
}
