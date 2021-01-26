package com.raquo.airstream.debug

import com.raquo.airstream.core.Observable
import com.raquo.airstream.util.always
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

trait DebugObservable[+A] extends Observable[A] {

  protected[this] def createDebugObservable(debugger: ObservableDebugger[A]): DebugSelf[A]

  protected[this] def sourceName: String

  // -- Callback spies --

  /** Execute fn on every emitted event or error */
  def spy(fn: Try[A] => Unit): DebugSelf[A] = {
    val debugger = ObservableDebugger(sourceName, topoRank, onFire = fn)
    createDebugObservable(debugger)
  }

  /** Execute fn on every emitted event (but not error) */
  def spyEvents(fn: A => Unit): DebugSelf[A] = {
    spy {
      case Success(ev) => fn(ev)
      case _ => ()
    }
  }

  /** Execute fn on every emitted error (but not regular events) */
  def spyErrors(fn: Throwable => Unit): DebugSelf[A] = {
    spy {
      case Failure(err) => fn(err)
      case _ => ()
    }
  }

  /** Execute callbacks on when the observable starts and stops
    *
    * @param startFn topoRank => ()
    */
  def spyLifecycle(startFn: Int => Unit, stopFn: () => Unit): DebugSelf[A] = {
    val debugger = ObservableDebugger(sourceName, topoRank, onStart = () => startFn(topoRank), onStop = stopFn)
    createDebugObservable(debugger)
  }

  /** Execute callbacks on when the observable starts
    *
    * @param fn topoRank => ()
    */
  def spyStarts(fn: Int => Unit): DebugSelf[A] = {
    spyLifecycle(startFn = fn, stopFn = () => ())
  }

  /** Execute callbacks on when the observable stops */
  def spyStops(fn: () => Unit): DebugSelf[A] = {
    spyLifecycle(startFn = _ => (), stopFn = fn)
  }

  // -- Logging --

  // @TODO[API] print with dom.console.log automatically only if a JS value detected? Not sure if possible to do well.

  /** Log emitted events and errors if `when` condition passes, using dom.console.log if `useJsLogger` is true. */
  def log(when: Try[A] => Boolean = always, useJsLogger: Boolean = false): DebugSelf[A] = {
    spy { value =>
      if (when(value)) {
        value match {
          case Success(ev) => _log("event", ev, useJsLogger)
          case Failure(err) => _log("error", err, useJsLogger)
        }
      }
    }
  }

  /** Log emitted events (but not errors) if `when` condition passes, using dom.console.log if `useJsLogger` is true. */
  def logEvents(when: A => Boolean = always, useJsLogger: Boolean = false): DebugSelf[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Success(ev) if when(ev) => true
      case _ => false
    }
    log(whenEvent, useJsLogger)
  }

  /** Log emitted errors (but not regular events) if `when` condition passes */
  def logErrors(when: Throwable => Boolean = always): DebugSelf[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Failure(err) if when(err) => true
      case _ => false
    }
    log(whenEvent, useJsLogger = false)
  }

  /** Log when the observable starts and stops */
  def logLifecycle: DebugSelf[A] = {
    spyLifecycle(
      startFn = topoRank => println(s"$sourceName [started]: topoRank = $topoRank"),
      stopFn = () => println(s"$sourceName [stopped]"),
    )
  }

  /** Log when the observable starts */
  def logStarts: DebugSelf[A] = {
    spyLifecycle(
      startFn = topoRank => println(s"$sourceName [started]: topoRank = $topoRank"),
      stopFn = () => (),
    )
  }

  /** Log when the observable stops */
  def logStops: DebugSelf[A] = {
    spyLifecycle(
      startFn = _ => (),
      stopFn = () => println(s"$sourceName [stopped]"),
    )
  }

  protected[this] def _log(action: String, value: Any, useJsLogger: Boolean): Unit = {
    val prefix = s"$sourceName [$action]:"
    if (useJsLogger) {
      // This is useful if you're emitting native JS objects, they will be printed to the console nicer
      dom.console.log(prefix, value.asInstanceOf[js.Any])
    } else {
      println(s"$prefix $value")
    }
  }

  // -- Trigger JS debugger --

  /** Trigger JS debugger for emitted events and errors if `when` passes */
  def break(when: Try[A] => Boolean = always): DebugSelf[A] = {
    spy { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted events (but not errors) if `when` passes */
  def breakEvents(when: A => Boolean = always): DebugSelf[A] = {
    spyEvents { ev =>
      if (when(ev)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted errors (but not events) if `when` passes */
  def breakErrors(when: Throwable => Boolean = always): DebugSelf[A] = {
    spyErrors { err =>
      if (when(err)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger when the observable starts and stops */
  def breakLifecycle: DebugSelf[A] = {
    spyLifecycle(
      startFn = _ => js.special.debugger(),
      stopFn = () => js.special.debugger()
    )
  }

  /** Trigger JS debugger when the observable starts */
  def breakStarts: DebugSelf[A] = {
    spyLifecycle(
      startFn = _ => js.special.debugger(),
      stopFn = () => ()
    )
  }

  /** Trigger JS debugger when the observable stops */
  def breakStops: DebugSelf[A] = {
    spyLifecycle(
      startFn = _ => (),
      stopFn = () => js.special.debugger()
    )
  }
}
