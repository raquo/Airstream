package com.raquo.airstream.debug

import com.raquo.airstream.core.Signal
import com.raquo.airstream.util.always

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

abstract class DebugSignal[+A](debugger: ObservableDebugger[A]) extends Signal[A] with DebugObservable[A] {

  override protected[this] def createDebugObservable(debugger: ObservableDebugger[A]): DebugSignal[A] = {
    new DebugWriteSignal[A](this, debugger)
  }

  override protected[this] def sourceName: String = debugger.sourceName

  /** Execute fn when signal is evaluating its initial value */
  def spyInitialEval(fn: Try[A] => Unit): DebugSignal[A] = {
    val debugger = ObservableDebugger(sourceName, topoRank, onInitialEval = fn)
    createDebugObservable(debugger)
  }

  /** Log when signal is evaluating its initial value (if `when` passes at that time) */
  def logInitialEval(when: Try[A] => Boolean = always, useJsLogger: Boolean = false): DebugSelf[A] = {
    spyInitialEval { value =>
      if (when(value)) {
        value match {
          case Success(ev) => _log("initial-eval[event]", ev, useJsLogger)
          case Failure(err) => _log("initial-eval[error]", err, useJsLogger)
        }
      }
    }
  }

  /** Trigger JS debugger when signal is evaluating its initial value (if `when` passes at that time) */
  def breakInitialEval(when: Try[A] => Boolean = always): DebugSelf[A] = {
    spyInitialEval { value =>
      if (when(value)) {
        js.special.debugger()
      }
    }
  }
}
