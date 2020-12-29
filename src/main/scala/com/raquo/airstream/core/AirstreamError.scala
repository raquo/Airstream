package com.raquo.airstream.core

import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js

sealed abstract class AirstreamError(message: String) extends Throwable(message)

// @TODO[Naming]
object AirstreamError {

  // @TODO[API] What kind of information do we want to capture to make recovery easier?

  // @TODO[API] Should this be its own error? If so, should now() also throw a special error?
//  case class VarUpdateError(cause: Throwable)
//    extends AirstreamError("VarUpdateError: unable to update errored Var; cause: " + cause.getMessage)

  case class ErrorHandlingError(error: Throwable, cause: Throwable)
    extends AirstreamError("ErrorHandlingError: " + error.getMessage + "; cause: " + cause.getMessage)

  // @TODO[API] Should we maybe get a special case for Combine2, like CombinedError2(Try[A], Try[B])? This would make it easier to recover from such errors
  case class CombinedError(causes: Seq[Option[Throwable]])
    extends AirstreamError("CombinedError: " + causes.flatten.map(_.getMessage).mkString("; "))

  case class ObserverError(error: Throwable) extends AirstreamError("ObserverError: " + error.getMessage)

  case class ObserverErrorHandlingError(error: Throwable, cause: Throwable) extends AirstreamError("ObserverErrorHandlingError: " + error.getMessage + "; cause: " + cause.getMessage)

  // --

  // @TODO[API] I feel like unhandled error reporting should live in its own object somewhere. But where?

  /** Unhandled error reporting is the last line of defense to report errors that would otherwise silently disappear into the void.
    *
    * We do not publish a stream of errors because:
    * a) we want to maximally disconnect it from the rest of Airstream's Transaction infrastructure
    * b) we want easier debugging, and thus a shorter stack trace between
    *
    * Instead, we provide a similar Observer-based API as described below.
    */
  private[this] val unhandledErrorCallbacks = mutable.Buffer[Throwable => Unit]()

  /** Note: In IE, console is not defined unless the developer tools console is actually open.
    *       Some test environments might be lacking the console as well (e.g. node.js without jsdom).
    */
  val consoleErrorCallback: Throwable => Unit = { err =>
    try {
      dom.console.error(err.getMessage + "\n" + err.getStackTrace.mkString("\n"))
    } catch {
      case _: Throwable => ()
    }
  }

  // @TODO[API] Due to browser optimizations, function argument (err) might not be available in the console if it's not used in code. See if we run into this problem in practice.
  val debuggerErrorCallback: Throwable => Unit = { _ =>
    js.special.debugger()
  }

  /** Note: this callback is allowed to throw, it is treated specially in
    * `sendUnhandledError` such that other callbacks might not run after it.
    * It's useful to fail tests in case of unhandled errors.
    */
  val unsafeRethrowErrorCallback: Throwable => Unit = { err =>
    dom.console.warn("Using unsafe rethrow error callback. Note: other registered error callbacks might not run. Use with caution.")
    throw err
  }

  /** The safe way to rethrow an unhandled error */
  val delayedRethrowErrorCallback: Throwable => Unit = { err =>
    js.timers.setTimeout(0)(throw err)
    ()
  }

  def registerUnhandledErrorCallback(fn: Throwable => Unit): Unit = {
    unhandledErrorCallbacks.append(fn)
    ()
  }

  def unregisterUnhandledErrorCallback(fn: Throwable => Unit): Unit = {
    val ix = unhandledErrorCallbacks.indexOf(fn)
    if (ix >= 0) {
      unhandledErrorCallbacks.remove(ix)
      ()
    } else {
      throw new Exception("This function is not currently registered as unhandled error callback. Make sure you're not accidentally creating a new function value when calling this.")
    }
  }

  // @TODO[API,Integrity] How should we report errors here? Must make sure to not induce an infinite loop. Throw an error in a setTimeout?
  private[airstream] def sendUnhandledError(err: Throwable): Unit = {
    unhandledErrorCallbacks.foreach(fn => try {
      fn(err)
    } catch {
      case err: Throwable if fn == unsafeRethrowErrorCallback =>
        // Note: this does not let other error callbacks to execute
        throw err
      case err: Throwable =>
        dom.console.warn("Error processing an unhandled error callback:")
        js.timers.setTimeout(0)(throw err)
    })
  }

  /** To remove console logger, call .unregisterUnhandledErrorCallback(consoleErrorCallback) */
  registerUnhandledErrorCallback(consoleErrorCallback)
}
