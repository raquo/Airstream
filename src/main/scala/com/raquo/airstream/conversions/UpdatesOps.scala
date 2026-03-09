package com.raquo.airstream.conversions

import com.raquo.airstream.core.{EventStream, Signal}
import org.scalajs.dom.MouseEvent
import scala.util.Try

/**
 * A base type for observables that have a stream of updates (namely, only [[Signal]] and its subtypes).
 *
 * @tparam A The type of value emitted by this observable. (e.g. [[MouseEvent]] or [[Int]].)
 */
trait UpdatesOps[+A] {

  // #TODO[API] Why is .updates a def, and not a lazy val?
  //  See `signal.updates shouldNotBe signal.updates` in SignalSpec
  /** A stream of all values in this signal, excluding the initial value.
   *
   * When re-starting this stream, it emits the signal's new current value
   * if and only if something has caused the signal's value to be updated
   * or re-evaluated while the updates stream was stopped. This way the
   * updates stream stays in sync with the signal even after restarting.
   */
  def updates: EventStream[A]

  /** Get the signal's current value */
  protected[airstream] def tryNow(): Try[A]

  /** Modify the Signal's changes stream, e.g. signal.composeChanges(_.delay(ms = 100))
   *
   * Alias to changes(operator). See also: [[composeAll]]
   *
   * @param operator Note: Must not throw!
   */
  def composeUpdates[AA >: A](
    operator: EventStream[A] => EventStream[AA]
  ): Signal[AA] = {
    composeAll(updatesOperator = operator, initialOperator = identity)
  }

  /** Modify both the Signal's changes stream, and its initial.
   * Similar to composeChanges, but lets you output a type unrelated to A.
   *
   * @param updatesOperator Note: Must not throw!
   * @param initialOperator Note: Must not throw!
   */
  def composeAll[B](
    updatesOperator: EventStream[A] => EventStream[B],
    initialOperator: Try[A] => Try[B],
    cacheInitialValue: Boolean = false
  ): Signal[B] = {
    updatesOperator(updates).toSignalWithTry(initialOperator(tryNow()), cacheInitialValue)
  }

  /** Modify the Signal's updates, e.g. signal.updates(_.delay(ms = 100))
   *
   * Alias to [[composeUpdates]]. See also: [[composeAll]]
   *
   * @param compose Note: Must not throw!
   */
  @inline def updates[AA >: A](compose: EventStream[A] => EventStream[AA]): Signal[AA] = {
    composeUpdates(compose)
  }

  @deprecated("signal.composeChanges renamed to signal.composeUpdates", since = "18.0.0-M3")
  def composeChanges[AA >: A](
    operator: EventStream[A] => EventStream[AA]
  ): Signal[AA] = {
    composeUpdates(operator)
  }

  @deprecated("signal.changes renamed to signal.updates", since = "18.0.0-M3")
  def changes: EventStream[A] = updates

  @deprecated("signal.changes renamed to signal.updates", since = "18.0.0-M3")
  def changes[AA >: A](compose: EventStream[A] => EventStream[AA]): Signal[AA] = updates(compose)
}
