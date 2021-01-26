package com.raquo.airstream.debug

import com.raquo.airstream.core.Observer
import com.raquo.airstream.util.always
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** This implicit class provides debug* methods for observers.
  *
  * Every debug* method creates a new observer that performs the specified behaviour
  * and then calls the original observer.
  *
  * Note: type inference doesn't work on Observer debug* methods,
  * so you need to provide the AA type param (it should be same as A).
  */
class DebuggableObserver[A](val observer: Observer[A]) extends AnyVal {

  /** Create a new observer with a displayName, that sends all events
    * to the original observer. This is different from `setDisplayName`.
    *
    * If you say `observer.debugWithName("foo").debugLog()`, the displayName
    * used by the logging observer will be "foo" verbatim, whereas if you say
    * `observer.setDisplayName("foo").debugLog()`, the logger's displayName
    * will be "foo|Debug" – with a suffix – to differentiate it from
    * the "foo" displayName of `observer` itself.
    */
  def debugWithName(displayName: String): Observer[A] = {
    observer.debugSpy(_ => ()).setDisplayName(displayName)
  }

  // -- Spy with callbacks --

  /** Execute fn on every emitted event or error */
  def debugSpy(fn: Try[A] => Unit): Observer[A] = new DebuggerObserver[A](observer, fn)

  /** Execute fn on every emitted event (but not error) */
  def debugSpyEvents(fn: A => Unit): Observer[A] = {
    debugSpy {
      case Success(ev) => fn(ev)
      case _ => ()
    }
  }

  /** Execute fn on every emitted error (but not regular events) */
  def debugSpyErrors(fn: Throwable => Unit): Observer[A] = {
    debugSpy {
      case Failure(err) => fn(err)
      case _ => ()
    }
  }

  // -- Logging --

  // @TODO[API] print with dom.console.log automatically only if a JS value detected? Not sure if possible to do well.

  /** Log emitted events and errors if `when` condition passes, using dom.console.log if `useJsLogger` is true. */
  def debugLog(when: Try[A] => Boolean = always, useJsLogger: Boolean = false): Observer[A] = {
    debugSpy { value =>
      if (when(value)) {
        value match {
          case Success(ev) => log("event", ev, useJsLogger)
          case Failure(err) => log("error", err, useJsLogger)
        }
      }
    }
  }

  /** Log emitted events (but not errors) if `when` condition passes, using dom.console.log if `useJsLogger` is true. */
  def debugLogEvents(when: A => Boolean = always, useJsLogger: Boolean = false): Observer[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Success(ev) if when(ev) => true
      case _ => false
    }
    debugLog(whenEvent, useJsLogger)
  }

  /** Log emitted errors (but not regular events) if `when` condition passes */
  def debugLogErrors(when: Throwable => Boolean = always): Observer[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Failure(err) if when(err) => true
      case _ => false
    }
    debugLog(whenEvent, useJsLogger = false)
  }

  protected[this] def log(
    action: String,
    value: Any,
    useJsLogger: Boolean
  ): Unit = {
    val prefix = s"${observer.displayName} [$action]:"
    if (useJsLogger) {
      // This is useful if you're listening for native JS objects, they will be printed to the console nicer
      dom.console.log(prefix, value.asInstanceOf[js.Any])
    } else {
      println(s"$prefix $value")
    }
  }

  // -- Trigger JS debugger --

  /** Trigger JS debugger for emitted events and errors if `when` passes */
  def debugBreak(when: Try[A] => Boolean = always): Observer[A] = {
    debugSpy { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted events (but not errors) if `when` passes */
  def debugBreakEvents(when: A => Boolean = always): Observer[A] = {
    debugSpyEvents { ev =>
      if (when(ev)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted errors (but not events) if `when` passes */
  def debugBreakErrors(when: Throwable => Boolean = always): Observer[A] = {
    debugSpyErrors { err =>
      if (when(err)) {
        js.special.debugger()
      }
    }
  }
}
