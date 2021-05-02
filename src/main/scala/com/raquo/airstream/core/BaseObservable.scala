package com.raquo.airstream.core

import com.raquo.airstream.debug.Debugger
import com.raquo.airstream.flatten.FlattenStrategy
import com.raquo.airstream.ownership.{Owner, Subscription}

import scala.annotation.unused
import scala.util.Try

/** This trait represents a reactive value that can be subscribed to.
  *
  * It has only one direct subtype, [[Observable]], which in turn has two direct subtypes, [[EventStream]] and [[Signal]].
  *
  * [[BaseObservable]] is the same as [[Observable]], it just lives in a separate trait for technical reasons (the Self type param).
  *
  * All Observables are lazy. An Observable starts when it gets its first observer (internal or external),
  * and stops when it loses its last observer (again, internal or external).
  *
  * Basic idea: Lazy Observable only holds references to those children that have any observers
  * (either directly on themselves, or on any of their descendants). What this achieves:
  * - Stream only propagates its value to children that (directly or not) have observers
  * - Stream calculates its value only once regardless of how many observers / children it has)
  *   (so, all streams are "hot" observables)
  * - Stream doesn't hold references to Streams that no one observes, allowing those Streams
  *   to be garbage collected if they are otherwise unreachable (which they should become
  *   when their subscriptions are killed by their owners)
  */
trait BaseObservable[+Self[+_] <: Observable[_], +A] extends Source[A] with Named {

  @inline protected implicit def protectedAccessEvidence: Protected = Protected.protectedAccessEvidence

  /** Note: Use Protected.topoRank(observable) to read another observable's topoRank if needed */
  protected val topoRank: Int


  /** @param project Note: guarded against exceptions */
  def map[B](project: A => B): Self[B]

  /** `value` is passed by name, so it will be evaluated whenever the Observable fires.
    * Use it to sample mutable values (e.g. myInput.ref.value in Laminar).
    *
    * See also: [[mapToStrict]]
    *
    * @param value Note: guarded against exceptions
    */
  def mapTo[B](value: => B): Self[B] = map(_ => value)

  /** `value` is evaluated strictly, only once, when this method is called.
    *
    * See also: [[mapTo]]
    */
  def mapToStrict[B](value: B): Self[B] = map(_ => value)

  // @TODO[API] Not sure if `distinct` `should accept A => Key or (A, A) => Boolean. We'll start with a more constrained version for now.
  // @TODO[API] Implement this. We should consider making a slide() operator to support this

  /** Emit a value unless its key matches the key of the last emitted value */
  // def distinct[Key](key: A => Key): Self[A]

  /** @param compose Note: guarded against exceptions */
  @inline def flatMap[B, Inner[_], Output[+_] <: Observable[_]](compose: A => Inner[B])(
    implicit strategy: FlattenStrategy[Self, Inner, Output]
  ): Output[B] = {
    strategy.flatten(map(compose))
  }

  def toStreamIfSignal[B >: A](ifSignal: Signal[A] => EventStream[B]): EventStream[B] = {
    this match {
      case s: Signal[A @unchecked] => ifSignal(s)
      case s: EventStream[A @unchecked] => s
      case _ => throw new Exception("All Observables must extend EventStream or Signal")
    }
  }

  def toSignalIfStream[B >: A](ifStream: EventStream[A] => Signal[B]): Signal[B] = {
    this match {
      case s: EventStream[A @unchecked] => ifStream(s)
      case s: Signal[A @unchecked] => s
      case _ => throw new Exception("All Observables must extend EventStream or Signal")
    }
  }

  /** Convert this observable to a signal of Option[A]. If it is a stream, set initial value to None. */
  def toWeakSignal: Signal[Option[A]] = {
    map(Some(_)) match {
      case s: EventStream[Option[A @unchecked] @unchecked] => s.toSignal(initial = None)
      case s: Signal[Option[A @unchecked] @unchecked] => s
      case _ => throw new Exception("All Observables must extend EventStream or Signal")
    }
  }

  // @TODO[API] I don't like the Option[O] output type here very much. We should consider a sentinel error object instead (need to check performance). Or maybe add a recoverOrSkip method or something?
  /** @param pf Note: guarded against exceptions */
  def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Self[B]

  def recoverIgnoreErrors: Self[A] = recover[A]{ case _ => None }

  /** Convert this to an observable that emits Failure(err) instead of erroring */
  def recoverToTry: Self[Try[A]]

  /** Create a new observable that listens to this one and has a debugger attached.
    *
    * Use the resulting observable in place of the original observable in your code.
    * See docs for details.
    *
    * There are more convenient methods available implicitly from [[DebuggableObservable]] and [[DebuggableSignal]],
    * such as debugLog(), debugSpyEvents(), etc.
    */
  def debugWith(debugger: Debugger[A]): Self[A]

  /** Create an external observer from a function and subscribe it to this observable.
    *
    * Note: since you won't have a reference to the observer, you will need to call Subscription.kill() to unsubscribe
    * */
  def foreach(onNext: A => Unit)(implicit owner: Owner): Subscription = {
    val observer = Observer(onNext)
    addObserver(observer)(owner)
  }

  /** Subscribe an external observer to this observable */
  def addObserver(observer: Observer[A])(implicit owner: Owner): Subscription

  protected[this] def addExternalObserver(observer: Observer[A], owner: Owner): Subscription

  protected[this] def onAddedExternalObserver(@unused observer: Observer[A]): Unit = ()

  /** Child observable should call this method on its parents when it is started.
    * This observable calls [[onStart]] if this action has given it its first observer (internal or external).
    */
  protected[airstream] def addInternalObserver(observer: InternalObserver[A]): Unit

  /** Child observable should call Transaction.removeInternalObserver(parent, childInternalObserver) when it is stopped.
    * This observable calls [[onStop]] if this action has removed its last observer (internal or external).
    */
  protected[airstream] def removeInternalObserverNow(observer: InternalObserver[A]): Unit

  protected[airstream] def removeExternalObserverNow(observer: Observer[A]): Unit

  /** Total number of internal and external observers */
  protected def numAllObservers: Int

  protected def isStarted: Boolean = numAllObservers > 0

  /** This method is fired when this observable starts working (listening for parent events and/or firing its own events),
    * that is, when it gets its first Observer (internal or external).
    *
    * [[onStart]] can potentially be called multiple times, the second time being after it has stopped (see [[onStop]]).
    */
  protected def onStart(): Unit = ()

  /** This method is fired when this observable stops working (listening for parent events and/or firing its own events),
    * that is, when it loses its last Observer (internal or external).
    *
    * [[onStop]] can potentially be called multiple times, the second time being after it has started again (see [[onStart]]).
    */
  protected def onStop(): Unit = ()

}

object BaseObservable {

  @inline private[airstream] def topoRank[O[+_] <: Observable[_]](observable: BaseObservable[O, _]): Int = {
    observable.topoRank
  }
}
