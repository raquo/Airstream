package com.raquo.airstream.debug

import com.raquo.airstream.core.AirstreamError.DebugError
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.util.always
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

class DebugObserver[A](parent: Observer[A], debugger: ObserverDebugger[A]) extends Observer[A] {

  // -- Spy with callbacks --

  protected[this] def createDebugObserver(newOnFire: Try[A] => Unit): DebugObserver[A] = {
    val newDebugger = ObserverDebugger(debugger.sourceName, newOnFire)
    new DebugObserver[A](this, newDebugger)
  }

  /** Execute fn on every emitted event or error */
  def spy(fn: Try[A] => Unit): DebugObserver[A] = {
    createDebugObserver(fn)
  }

  /** Execute fn on every emitted event (but not error) */
  def spyEvents(fn: A => Unit): DebugObserver[A] = {
    spy {
      case Success(ev) => fn(ev)
      case _ => ()
    }
  }

  /** Execute fn on every emitted error (but not regular events) */
  def spyErrors(fn: Throwable => Unit): DebugObserver[A] = {
    spy {
      case Failure(err) => fn(err)
      case _ => ()
    }
  }

  // -- Logging --

  // @TODO[API] print with dom.console.log automatically only if a JS value detected? Not sure if possible to do well.

  /** Log emitted events and errors if `when` condition passes, using dom.console.log if `useJsLogger` is true. */
  def log(when: Try[A] => Boolean = always, useJsLogger: Boolean = false): DebugObserver[A] = {
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
  def logEvents(when: A => Boolean = always, useJsLogger: Boolean = false): DebugObserver[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Success(ev) if when(ev) => true
      case _ => false
    }
    log(whenEvent, useJsLogger)
  }

  /** Log emitted errors (but not regular events) if `when` condition passes */
  def logErrors(when: Throwable => Boolean = always): DebugObserver[A] = {
    val whenEvent = (value: Try[A]) => value match {
      case Failure(err) if when(err) => true
      case _ => false
    }
    log(whenEvent, useJsLogger = false)
  }

  private def _log(action: String, value: Any, useJsLogger: Boolean): Unit = {
    val prefix = s"${debugger.sourceName} [$action]:"
    if (useJsLogger) {
      // This is useful if you're listening to native JS objects, they will be printed to the console nicer
      dom.console.log(prefix, value.asInstanceOf[js.Any])
    } else {
      println(s"$prefix $value")
    }
  }

  // -- Trigger JS debugger --

  /** Trigger JS debugger for emitted events and errors if `when` passes */
  def break(when: Try[A] => Boolean = always): DebugObserver[A] = {
    spy { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted events (but not errors) if `when` passes */
  def breakEvents(when: A => Boolean = always): DebugObserver[A] = {
    spyEvents { ev =>
      if (when(ev)) {
        js.special.debugger()
      }
    }
  }

  /** Trigger JS debugger for emitted errors (but not events) if `when` passes */
  def breakErrors(when: Throwable => Boolean = always): DebugObserver[A] = {
    spyErrors { err =>
      if (when(err)) {
        js.special.debugger()
      }
    }
  }

  override def onTry(nextValue: Try[A]): Unit = {
    try {
      debugger.onFire(nextValue)
    } catch {
      case err: Throwable =>
        val maybeCause = nextValue.toEither.left.toOption
        AirstreamError.sendUnhandledError(DebugError(err, cause = maybeCause))
    }
    parent.onTry(nextValue)
  }

  final override def onNext(nextValue: A): Unit = {
    onTry(Success(nextValue))
  }

  final override def onError(err: Throwable): Unit = {
    onTry(Failure(err))
  }
}
