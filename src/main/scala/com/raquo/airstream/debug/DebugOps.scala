package com.raquo.airstream.debug

import com.raquo.airstream.core.{Named, Observable}
import com.raquo.airstream.util.always
import org.scalajs.dom

import scala.annotation.nowarn
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** This trait provides debug* methods for observables, e.g.:
  *
  * {{{
  * stream.debugWithName("MyStream").debugSpyStarts().debugLogEvents()
  * }}}
  *
  * The result of the chain is an observable that you should use in place
  * of the original observable (`stream` in this case).
  *
  * The implicit conversion to this class is defined in the [[Observable]] companion object.
  *
  * See also: [[DebugSignalOps]] for signal-specific debug helpers
  */
trait DebugOps[+Self[+_] <: Named, +A] {
  this: Named =>

  /** Return the observable's topoRank. This does not affect the observable in any way. */
  def debugTopoRank: Int

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
    debugSpyAll().setDisplayName(displayName)
  }

  // -- Callback spies --

  /** Execute fn on every emitted event or error
    * Note: for Signals this also triggers onStart (with the current value at the time)
    */
  def debugSpy(onFire: Try[A] => Unit): Self[A] = {
    debugSpyAll(onFire = onFire)
  }

  /** Execute fn on every emitted event (but not error)
    * Note: for Signals this also triggers onStart (if current value is not an error)
    */
  def debugSpyEvents(onEvent: A => Unit): Self[A] = {
    debugSpy {
      case Success(ev) => onEvent(ev)
      case _ => ()
    }
  }

  /** Execute fn on every emitted error (but not regular events)
    * Note: for Signals this also triggers onStart (if current value is an error)
    */
  def debugSpyErrors(onError: Throwable => Unit): Self[A] = {
    debugSpy {
      case Failure(err) => onError(err)
      case _ => ()
    }
  }

  /** Execute callbacks when the observable starts and stops
    *
    * @param onStart topoRank => ()
    */
  def debugSpyLifecycle(onStart: Int => Unit, onStop: () => Unit): Self[A] = {
    debugSpyAll(
      onStart = onStart,
      onStop = onStop
    )
  }

  /** Execute callbacks when the observable starts
    *
    * @param onStart topoRank => ()
    */
  def debugSpyStarts(onStart: Int => Unit): Self[A] = {
    debugSpyLifecycle(onStart = onStart, onStop = () => ())
  }

  /** Execute callbacks when the observable stops */
  def debugSpyStops(onStop: () => Unit): Self[A] = {
    debugSpyLifecycle(onStart = _ => (), onStop = onStop)
  }

  /** Execute callbacks on any of the supported hooks.
    * Note that for signals, `onFire` also fires onStart with the current value. // #TODO[API] does not seem very robust...
    * Note that `onEvalFromParent` is only applicable to signals, not streams.
    */
  def debugSpyAll(
    onStart: Int => Unit = _ => (),
    onStop: () => Unit = () => (),
    onFire: Try[A] => Unit = _ => (),
    onEvalFromParent: Try[A] => Unit = _ => ()
  ): Self[A] = {
    (debugWith(
      Debugger(
        onStart = () => onStart(debugTopoRank),
        onStop = onStop,
        onFire = onFire,
        onEvalFromParent = onEvalFromParent
      )
    ): @nowarn("msg=deprecated"))
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
    val whenEvent = (value: Try[A]) =>
      value match {
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
    val whenEvent = (value: Try[A]) =>
      value match {
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
      onStart = topoRank => {
        if (logStarts) {
          log("started", Some(s"topoRank = $topoRank"), useJsLogger = false)
        }
      },
      onStop = () => {
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

  /** Debug log everything. */
  def debugLogAll(useJsLogger: Boolean = false): Self[A] = {
    debugSpyAll(
      onStart = topoRank => {
        log("started", Some(s"topoRank = $topoRank"), useJsLogger = false)
      },
      onStop = () => {
        log("stopped", value = None, useJsLogger = false)
      },
      onFire = {
        case Success(ev) => log("event", Some(ev), useJsLogger)
        case Failure(err) => log("error", Some(err), useJsLogger)
      },
      onEvalFromParent = {
        case Success(ev) => log("eval-from-parent[value]", Some(ev), useJsLogger)
        case Failure(err) => log("eval-from-parent[error]", Some(err), useJsLogger)
      }
    )
  }

  protected[this] def log(
    action: String,
    value: Option[Any],
    useJsLogger: Boolean
  ): Unit = {
    val maybeColon = if (value.isDefined) ":" else ""
    val prefix = s"${displayName} [$action]$maybeColon"
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
      onStart = _ => js.special.debugger(),
      onStop = () => js.special.debugger()
    )
  }

  /** Trigger JS debugger when the observable starts */
  def debugBreakStarts: Self[A] = {
    debugSpyLifecycle(
      onStart = _ => js.special.debugger(),
      onStop = () => ()
    )
  }

  /** Trigger JS debugger when the observable stops */
  def debugBreakStops: Self[A] = {
    debugSpyLifecycle(
      onStart = _ => (),
      onStop = () => js.special.debugger()
    )
  }

  // #TODO[API] Remove this and switch to debugSpyAll being the abstract method
  /** Create a new observable that listens to this one and has a debugger attached.
    *
    * Use the resulting observable in place of the original observable in your code.
    * See docs for details.
    *
    * There are more convenient methods available from [[DebugOps]] and [[DebugSignalOps]],
    * such as debugLog(), debugSpyEvents(), etc.
    */
  @deprecated("Use debugSpyAll instead of debugWith", since = "18.0.0-M3")
  def debugWith(debugger: Debugger[A]): Self[A]
}
