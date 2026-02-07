package com.raquo.airstream.debug

import com.raquo.airstream.core.{BaseObservable, Signal}
import com.raquo.airstream.util.always

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/** This trait provides Signal-specific debug* methods, e.g.:
  *
  * {{{
  * signal.debugLogInitialEval().debugLog()
  * }}}
  *
  * See [[DebugOps]] and the docs for details.
  *
  * The implicit conversion to this class is defined in the [[Signal]] companion object.
  *
  * See also [[DebugOps]] for generic debug operators
  */
trait DebugSignalOps[+A]
extends DebugOps[Signal, A] {
  this: BaseObservable[Signal, A] =>

  /** Execute fn when signal is evaluating its `currentValueFromParent`.
    * This is typically triggered when evaluating signal's initial value onStart,
    * as well as on subsequent re-starts when the signal is syncing its value
    * to the parent's new current value. */
  def debugSpyEvalFromParent(fn: Try[A] => Unit): Signal[A] = {
    val debugger = Debugger(onEvalFromParent = fn)
    debugWith(debugger)
  }

  /** Log when signal is evaluating its initial value (if `when` passes at that time) */
  def debugLogEvalFromParent(
    when: Try[A] => Boolean = always,
    useJsLogger: Boolean = false
  ): Signal[A] = {
    debugSpyEvalFromParent { value =>
      if (when(value)) {
        value match {
          case Success(ev) => log("eval-from-parent", Some(ev), useJsLogger)
          case Failure(err) => log("eval-from-parent[error]", Some(err), useJsLogger)
        }
      }
    }
  }

  /** Trigger JS debugger when signal is evaluating its initial value (if `when` passes at that time) */
  def debugBreakEvalFromParent(when: Try[A] => Boolean = always): Signal[A] = {
    debugSpyEvalFromParent { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }
}
