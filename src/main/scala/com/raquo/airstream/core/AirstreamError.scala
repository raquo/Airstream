package com.raquo.airstream.core

import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js

sealed abstract class AirstreamError(message: String) extends Throwable(message)

object AirstreamError {

  def getFullMessage(e: Throwable): String = {
    // getMessage can have a custom implementation, and thus can throw
    // Also, currently, MatchError.getMessage throws an NPE
    // if the object being matched is a native JS type.
    val errorMessage =
      try {
        e.getMessage
      } catch {
        case err: Throwable =>
          // Don't try to read anything from `err` here, things are f*cked as they are already.
          "(Unable to get the message for this error - exception occurred in its getMessage)"
      }
    e.getClass.getSimpleName + ": " + errorMessage
  }

  def getStackTrace(err: Throwable, newline: String): String = {
    // See comments in getFullMessage - same applies here.
    try {
      err.getStackTrace.mkString(newline)
    } catch {
      case err: Throwable =>
        "(Unable to get the stacktrace for this error - exception occurred in its getStackTrace)"
    }
  }

  case class VarError(message: String, cause: Option[Throwable])
    extends AirstreamError(s"$message; cause: ${cause.map(getFullMessage)}") {

    cause.foreach(initCause)

    override def toString: String = s"VarError: $message; cause: $cause"
  }

  case class ErrorHandlingError(error: Throwable, cause: Throwable)
    extends AirstreamError(s"ErrorHandlingError: ${getFullMessage(error)}; cause: ${getFullMessage(cause)}") {

    initCause(cause)

    override def toString: String = s"ErrorHandlingError: $error; cause: $cause"
  }

  case class CombinedError(causes: Seq[Option[Throwable]])
    extends AirstreamError(s"CombinedError: ${causes.flatten.map(getFullMessage).mkString("; ")}") {

    causes.flatten.headOption.foreach(initCause) // Just get the first cause – better than nothing I guess?

    override def toString: String = s"CombinedError: ${causes.flatten.toList.mkString("; ")}"
  }

  case class ObserverError(error: Throwable) extends AirstreamError(s"ObserverError: ${getFullMessage(error)}") {

    override def toString: String = s"ObserverError: $error"
  }

  case class ObserverErrorHandlingError(error: Throwable, cause: Throwable)
    extends AirstreamError(s"ObserverErrorHandlingError: ${getFullMessage(error)}; cause: ${getFullMessage(cause)}") {

    initCause(cause)

    override def toString: String = s"ObserverErrorHandlingError: $error; cause: $cause"
  }

  case class DebugError(error: Throwable, cause: Option[Throwable])
    extends AirstreamError(s"DebugError: ${getFullMessage(error)}; cause: ${cause.map(getFullMessage)}") {

    override def toString: String = s"DebugError: $error; cause: $cause"
  }

  case class TransactionDepthExceeded(trx: Transaction, depth: Int)
    extends AirstreamError(s"Transaction depth exceeded maxDepth = $depth: Execution of $trx aborted. See `Transaction.maxDepth`.") {

    override def toString: String = s"TransactionDepthExceeded: $trx; maxDepth: $depth"
  }

  // --

  // @TODO[API] I feel like unhandled error reporting should live in its own object somewhere. But where?

  /** Unhandled error reporting is the last line of defense to report errors that would otherwise silently disappear into the void.
    *
    * We do not publish a stream of errors because:
    * a) we want to maximally disconnect it from the rest of Airstream's Transaction infrastructure
    * b) we want easier debugging, and thus a shorter stack trace
    *
    * Instead, we provide a similar Observer-based API as described below.
    */
  private[this] val unhandledErrorCallbacks = mutable.Buffer[Throwable => Unit]()

  /** Note: In IE, console is not defined unless the developer tools console is actually open.
    * Some test environments might be lacking the console as well (e.g. node.js without jsdom).
    */
  val consoleErrorCallback: Throwable => Unit = { err =>
    try {
      dom.console.error(getFullMessage(err) + "\n" + getStackTrace(err, newline = "\n"))
    } catch {
      case err: Throwable =>
        // If you ever hit this, you will _really_ appreciate this printout.
        dom.console.error("Error in AirstreamError.consoleErrorCallback:")
        dom.console.error(err)
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
  }

  def registerUnhandledErrorCallback(fn: Throwable => Unit): Unit = {
    unhandledErrorCallbacks.append(fn)
  }

  def unregisterUnhandledErrorCallback(fn: Throwable => Unit): Unit = {
    val ix = unhandledErrorCallbacks.indexOf(fn)
    if (ix >= 0) {
      unhandledErrorCallbacks.remove(ix)
    } else {
      throw new Exception("This function is not currently registered as unhandled error callback. Make sure you're not accidentally creating a new function value when calling this.")
    }
  }

  // @TODO[API,Integrity] How should we report errors here? Must make sure to not induce an infinite loop. Throw an error in a setTimeout?
  def sendUnhandledError(err: Throwable): Unit = {
    unhandledErrorCallbacks.foreach { fn =>
      try {
        fn(err)
      } catch {
        case err: Throwable if fn == unsafeRethrowErrorCallback =>
          // Note: this does not let other error callbacks execute
          throw err
        case err: Throwable =>
          dom.console.warn("Error processing an unhandled error callback:")
          js.timers.setTimeout(0)(throw err)
      }
    }
  }

  /** To remove console logger, call .unregisterUnhandledErrorCallback(consoleErrorCallback) */
  registerUnhandledErrorCallback(consoleErrorCallback)
}
