package com.raquo.airstream.core

import com.raquo.airstream.flatten.FlattenStrategy
import com.raquo.airstream.flatten.FlattenStrategy.{SwitchFutureStrategy, SwitchSignalStrategy, SwitchStreamStrategy}
import com.raquo.airstream.ownership.{Owner, Subscription}

import scala.annotation.unused
import scala.concurrent.Future
import scala.util.Try

/** This trait represents a reactive value that can be subscribed to.
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
trait Observable[+A] {

  type Self[+T] <: Observable[T]

  // @TODO[API] This needs smarter permissions. See Laminar's DomEventStream
  protected[airstream] val topoRank: Int

  /** Create an external observer from a function and subscribe it to this observable.
    *
    * Note: since you won't have a reference to the observer, you will need to call Subscription.kill() to unsubscribe
    * */
  def foreach(onNext: A => Unit)(implicit owner: Owner): Subscription

  /** Subscribe an external observer to this observable */
  def addObserver(observer: Observer[A])(implicit owner: Owner): Subscription

  /** Child observable should call this method on its parents when it is started.
    * This observable calls [[onStart]] if this action has given it its first observer (internal or external).
    */
  protected[airstream] def addInternalObserver(observer: InternalObserver[A]): Unit

  /** Child observable should call Transaction.removeInternalObserver(parent, childInternalObserver) when it is stopped.
    * This observable calls [[onStop]] if this action has removed its last observer (internal or external).
    */
  protected[airstream] def removeInternalObserverNow(observer: InternalObserver[A]): Unit

  protected[airstream] def removeExternalObserverNow(observer: Observer[A]): Unit

  /** @param project Note: guarded against exceptions */
  def map[B](project: A => B): Self[B]

  /** `value` is passed by name, so it will be evaluated whenever the Observable fires.
    * Use it to sample mutable values (e.g. myInput.ref.value in Laminar).
    *
    * See also: [[mapToValue]]
    *
    * @param value Note: guarded against exceptions
    */
  def mapTo[B](value: => B): Self[B] = map(_ => value)

  /** `value` is evaluated only once, when this method is called.
    *
    * See also: [[mapTo]]
    */
  def mapToValue[B](value: B): Self[B] = map(_ => value)

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

  def toSignal[AA >: A](initialIfStream: => AA): Signal[AA] = {
    this match {
      case s: Signal[A @unchecked] => s
      case s: EventStream[A @unchecked] => s.startWith(initialIfStream)
    }
  }

  def toStreamOrSignalChanges: EventStream[A] = {
    this match {
      case s: EventStream[A @unchecked] => s
      case s: Signal[A @unchecked] => s.changes
    }
  }

  // @TODO[API] I don't like the Option[O] output type here very much. We should consider a sentinel error object instead (need to check performance). Or maybe add a recoverOrSkip method or something?
  /** @param pf Note: guarded against exceptions */
  def recover[B >: A](pf: PartialFunction[Throwable, Option[B]]): Self[B]

  def recoverIgnoreErrors: Self[A] = recover[A]{ case _ => None }

  /** Convert this to an observable that emits Failure(err) instead of erroring */
  def recoverToTry: Self[Try[A]]

  @inline protected def onAddedExternalObserver(@unused observer: Observer[A]): Unit = ()

  /** This method is fired when this observable starts working (listening for parent events and/or firing its own events),
    * that is, when it gets its first Observer (internal or external).
    *
    * [[onStart]] can potentially be called multiple times, the second time being after it has stopped (see [[onStop]]).
    */
  @inline protected def onStart(): Unit = ()

  /** This method is fired when this observable stops working (listening for parent events and/or firing its own events),
    * that is, when it loses its last Observer (internal or external).
    *
    * [[onStop]] can potentially be called multiple times, the second time being after it has started again (see [[onStart]]).
    */
  @inline protected def onStop(): Unit = ()


  // @TODO[API] print with dom.console.log automatically only if a JS value detected? Not sure if possible to do well.

  /** print events using println - use for Scala values */
//  def debugLog(prefix: String = "event", when: A => Boolean = _ => true): Self[A] = {
//    map(value => {
//      if (when(value)) {
//        println(prefix + ": " + value.asInstanceOf[js.Any])
//      }
//      value
//    })
//  }

  /** print events using dom.console.log - use for JS values */
//  def debugLogJs(prefix: String = "event", when: A => Boolean = _ => true): Self[A] = {
//    map(value => {
//      if (when(value)) {
//        dom.console.log(prefix + ": ", value.asInstanceOf[js.Any])
//      }
//      value
//    })
//  }

//  def debugBreak(when: A => Boolean = _ => true): Self[A] = {
//    map(value => {
//      if (when(value)) {
//        js.special.debugger()
//      }
//      value
//    })
//  }

  def debugSpy(fn: A => Unit): Self[A] = {
    map(value => {
      fn(value)
      value
    })
  }

  /** Print when the observable has just started or stopped */
  def debugLogLifecycle(prefix: String): Self[A]

  /** Run callbacks when the observable has just started or stopped */
  def debugSpyLifecycle(start: () => Unit = () => (), stop: () => Unit = () => ()): Self[A]

}

object Observable {

  implicit val switchStreamStrategy: FlattenStrategy[Observable, EventStream, EventStream] = SwitchStreamStrategy

  implicit val switchSignalStrategy: FlattenStrategy[Signal, Signal, Signal] = SwitchSignalStrategy

  implicit val switchFutureStrategy: FlattenStrategy[Observable, Future, EventStream] = SwitchFutureStrategy

  // @TODO[Elegance] Maybe use implicit evidence on a method instead?
  implicit class MetaObservable[A, Outer[+_] <: Observable[_], Inner[_]](
    val parent: Outer[Inner[A]]
  ) extends AnyVal {

    @inline def flatten[Output[+_] <: Observable[_]](
      implicit strategy: FlattenStrategy[Outer, Inner, Output]
    ): Output[A] = {
      strategy.flatten(parent)
    }
  }
}
