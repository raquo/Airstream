package com.raquo.airstream.debug

import com.raquo.airstream.core.{Protected, Signal}
import com.raquo.airstream.util.always

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** This implicit class provides Signal-specific debug* methods, e.g.:
  *
  *     signal.debugLogInitialEval().debugLog()
  *
  * See [[DebuggableObservable]] and the docs for details.
  *
  * The implicit conversion to this class is defined in the [[Signal]] companion object.
  *
  * This is not a value class because it needs to extend [[DebuggableObservable]].
  * The performance penalty of one extra instantiation per debugged stream should
  * not be noticeable.
  */
class DebuggableSignal[+A](override val observable: Signal[A]) extends DebuggableObservable[Signal, A](observable) {

  /** Execute fn when signal is evaluating its initial value */
  def debugSpyInitialEval(fn: Try[A] => Unit): Signal[A] = {
    val debugger = Debugger(Protected.topoRank(observable), onInitialEval = fn)
    observable.debugWith(debugger)
  }

  /** Log when signal is evaluating its initial value (if `when` passes at that time) */
  def debugLogInitialEval(
    when: Try[A] => Boolean = always,
    useJsLogger: Boolean = false
  ): Signal[A] = {
    debugSpyInitialEval { value =>
      if (when(value)) {
        value match {
          case Success(ev) => log("initial-eval[event]", Some(ev), useJsLogger)
          case Failure(err) => log("initial-eval[error]", Some(err), useJsLogger)
        }
      }
    }
  }

  /** Trigger JS debugger when signal is evaluating its initial value (if `when` passes at that time) */
  def debugBreakInitialEval(when: Try[A] => Boolean = always): Signal[A] = {
    debugSpyInitialEval { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }
}
