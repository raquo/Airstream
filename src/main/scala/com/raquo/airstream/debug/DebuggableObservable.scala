package com.raquo.airstream.debug

import com.raquo.airstream.core.{BaseObservable, Observable, Protected}
import com.raquo.airstream.util.always
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** This implicit class provides debug* methods for observables, e.g.:
  *
  *     stream.debugWithName("MyStream").debugSpyStarts().debugLogEvents()
  *
  * The result of the chain is an observable that you should use in place
  * of the original observable (`stream` in this case).
  *
  * The implicit conversion to this class is defined in the [[Observable]] companion object.
  *
  * This is not a value class because [[DebuggableSignal]] needs to extend this.
  * The performance penalty of one extra instantiation per debugged stream should
  * not be noticeable.
  */
class DebuggableObservable[Self[+_] <: Observable[_], +A](val observable: BaseObservable[Self, A]) {

  /** Return the observable's topoRank. This does not affect the observable in any way. */
  def debugTopoRank: Int = Protected.topoRank(observable)

  /** Create a new observable that listens to the original, and
    * set the displayName of the new observable.
    * This is different from `setDisplayName`.
    *
    * If you say `stream.debugWithName("foo").debugLog()`, the displayName
    * used by the logger will be "foo" verbatim, whereas if you say
    * `stream.setDisplayName("foo").debugLog()`, the logger's displayName
    * will be "foo|Debug" – with a suffix – to differentiate it from
    * the "foo" displayName of `stream` itself.
    */
  def debugWithName(displayName: String): Self[A] = {
    val emptyDebugger = Debugger()
    observable.debugWith(emptyDebugger).setDisplayName(displayName)
  }

  // -- Callback spies --

  /** Execute fn on every emitted event or error
    * Note: for Signals this also triggers onStart (with the current value at the time)
    */
  def debugSpy(fn: Try[A] => Unit): Self[A] = {
    val debugger = Debugger(onFire = fn)
    observable.debugWith(debugger)
  }

  /** Execute fn on every emitted event (but not error)
    * Note: for Signals this also triggers onStart (if current value is not an error)
    */
  def debugSpyEvents(fn: A => Unit): Self[A] = {
    debugSpy {
      case Success(ev) => fn(ev)
      case _ => ()
    }
  }

  /** Execute fn on every emitted error (but not regular events)
    * Note: for Signals this also triggers onStart (if current value is an error)
    */
  def debugSpyErrors(fn: Throwable => Unit): Self[A] = {
    debugSpy {
      case Failure(err) => fn(err)
      case _ => ()
    }
  }

  /** Execute callbacks on when the observable starts and stops
    *
    * @param startFn topoRank => ()
    */
  def debugSpyLifecycle(startFn: Int => Unit, stopFn: () => Unit): Self[A] = {
    val debugger = Debugger(
      onStart = () => startFn(Protected.topoRank(observable)),
      onStop = stopFn
    )
    observable.debugWith(debugger)
  }

  /** Execute callbacks on when the observable starts
    *
    * @param fn topoRank => ()
    */
  def debugSpyStarts(fn: Int => Unit): Self[A] = {
    debugSpyLifecycle(startFn = fn, stopFn = () => ())
  }

  /** Execute callbacks on when the observable stops */
  def debugSpyStops(fn: () => Unit): Self[A] = {
    debugSpyLifecycle(startFn = _ => (), stopFn = fn)
  }

  // -- Logging --

  // @TODO[API] print with dom.console.log automatically only if a JS value detected? Not sure if possible to do well.

  /** Log emitted events and errors if `when` condition passes, using dom.console.log if `useJsLogger` is true.
    * Note: for Signals this also triggers on start (with the current value at the time)
    */
  def debugLog(
    when: Try[A] => Boolean = always,
    useJsLogger: Boolean = false
  ): Self[A] = {
    debugSpy { value =>
      if (when(value)) {
        value match {
          case Success(ev) => log("event", Some(ev), useJsLogger)
          case Failure(err) => log("error", Some(err), useJsLogger)
        }
      }
    }
  }

  /** Log emitted events (but not errors) if `when` condition passes, using dom.console.log if `useJsLogger` is true.
    * Note: for Signals this also triggers onStart (if current value is not an error)
    */
  def debugLogEvents(
    when: A => Boolean = always,
    useJsLogger: Boolean = false
  ): Self[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Success(ev) if when(ev) => true
      case _ => false
    }
    debugLog(whenEvent, useJsLogger)
  }

  /** Log emitted errors (but not regular events) if `when` condition passes
    * Note: for Signals this also triggers onStart (if current value is an error)
    */
  def debugLogErrors(
    when: Throwable => Boolean = always
  ): Self[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Failure(err) if when(err) => true
      case _ => false
    }
    debugLog(whenEvent, useJsLogger = false)
  }

  /** Log when the observable starts and stops */
  def debugLogLifecycle(
    logStarts: Boolean = true,
    logStops: Boolean = true
  ): Self[A] = {
    debugSpyLifecycle(
      startFn = topoRank => {
        if (logStarts) {
          log("started", Some(s"topoRank = $topoRank"), useJsLogger = false)
        }
      },
      stopFn = () => {
        if (logStops) {
          log("stopped", value = None, useJsLogger = false)
        }
      },
    )
  }

  /** Log when the observable starts */
  def debugLogStarts: Self[A] = debugLogLifecycle(logStops = false)

  /** Log when the observable stops */
  def debugLogStops: Self[A] = debugLogLifecycle(logStarts = false)

  protected[this] def log(
    action: String,
    value: Option[Any],
    useJsLogger: Boolean
  ): Unit = {
    val maybeColon = if (value.isDefined) ":" else ""
    val prefix = s"${observable.displayName} [$action]$maybeColon"
    if (useJsLogger) {
      // This is useful if you're emitting native JS objects, they will be printed to the console nicer
      if (value.isDefined) {
        dom.console.log(prefix, value.get.asInstanceOf[js.Any])
      } else {
        dom.console.log(prefix)
      }
    } else {
      if (value.isDefined) {
        println(s"$prefix ${value.get}")
      } else {
        println(prefix)
      }
    }
  }

  // -- Trigger JS debugger --

  /** Trigger JS debugger for emitted events and errors if `when` passes
    * Note: for Signals this also triggers onStart (with the current value at the time)
    */
  def debugBreak(when: Try[A] => Boolean = always): Self[A] = {
    debugSpy { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted events (but not errors) if `when` passes
    * Note: for Signals this also triggers onStart (if current value is not an error)
    */
  def debugBreakEvents(when: A => Boolean = always): Self[A] = {
    debugSpyEvents { ev =>
      if (when(ev)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted errors (but not events) if `when` passes
    * Note: for Signals this also triggers onStart (if current value is an error)
    */
  def debugBreakErrors(when: Throwable => Boolean = always): Self[A] = {
    debugSpyErrors { err =>
      if (when(err)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger when the observable starts and stops */
  def debugBreakLifecycle: Self[A] = {
    debugSpyLifecycle(
      startFn = _ => js.special.debugger(),
      stopFn = () => js.special.debugger()
    )
  }

  /** Trigger JS debugger when the observable starts */
  def debugBreakStarts: Self[A] = {
    debugSpyLifecycle(
      startFn = _ => js.special.debugger(),
      stopFn = () => ()
    )
  }

  /** Trigger JS debugger when the observable stops */
  def debugBreakStops: Self[A] = {
    debugSpyLifecycle(
      startFn = _ => (),
      stopFn = () => js.special.debugger()
    )
  }
}
