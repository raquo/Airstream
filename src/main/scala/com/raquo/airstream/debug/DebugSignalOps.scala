package com.raquo.airstream.debug

import com.raquo.airstream.core.{Named, Signal}
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
trait DebugSignalOps[+Self[+_] <: Signal[_], +A]
extends DebugOps[Self, A] {
  this: Named =>

  /** Execute fn when signal is evaluating its `currentValueFromParent`.
    * This is typically triggered when getting a LazyStrictSignal's .now(), or
    * evaluating signal's initial value onStart, as well as on subsequent
    * re-starts when the signal is syncing its value to the parent's new
    * current value. */
  def debugSpyEvalFromParent(onEvalFromParent: Try[A] => Unit): Self[A] = {
    debugSpyAll(onEvalFromParent = onEvalFromParent)
  }

  /** Log when signal is evaluating its initial value (if `when` passes at that time) */
  def debugLogEvalFromParent(
    when: Try[A] => Boolean = always,
    useJsLogger: Boolean = false
  ): Self[A] = {
    debugSpyEvalFromParent { value =>
      if (when(value)) {
        value match {
          case Success(ev) => log("eval-from-parent[value]", Some(ev), useJsLogger)
          case Failure(err) => log("eval-from-parent[error]", Some(err), useJsLogger)
        }
      }
    }
  }

  /** Trigger JS debugger when signal is evaluating its initial value (if `when` passes at that time) */
  def debugBreakEvalFromParent(when: Try[A] => Boolean = always): Self[A] = {
    debugSpyEvalFromParent { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }
}
