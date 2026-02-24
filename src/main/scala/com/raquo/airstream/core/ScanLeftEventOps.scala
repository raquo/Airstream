package com.raquo.airstream.core

import com.raquo.airstream.core.Source.EventSource
import com.raquo.airstream.misc.ScanLeftSignal
import javax.swing.text.html.HTMLEditorKit.Parser
import org.scalajs.dom.svg.A

import scala.util.{Success, Try}

trait ScanLeftEventOps[+Self[+B] <: EventStream[B], +A]
extends BaseObservable[Self, A]
with EventSource[A] {

  // @TODO[API] Should we introduce some kind of FoldError() wrapper?
  /** A signal that emits the accumulated value every time that the parent stream emits.
   *
   * See also: [[EventStream.startWith]]
   *
   * @param fn Note: guarded against exceptions
   */
  def scanLeft[B](initial: B)(fn: (B, A) => B): Signal[B] = {
    scanLeftRecover(Success(initial)) { (currentValue, nextParentValue) =>
      Try(fn(currentValue.get, nextParentValue.get))
    }
  }

  /** A signal that emits the accumulated value every time that the parent stream emits.
   *
   * @param fn Note: Must not throw!
   */
  def scanLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B]

  /**
   * Creates an event stream that combines all events from the parent stream using [[fn]],
   * and emits the accumulated value every time that the parent stream emits.
   *
   * @param fn
   *   A binary operator that takes the previously accumulated value (left) and the next event from the parent stream (right),
   *   and produces the next accumulated value.
   *
   * @return
   *   An [[EventStream]] that emits the accumulated value every time that the parent stream emits.
   *
   * @see [[EventStream.scanLeft]]
   *   for a similar operator that also allows you to specify an initial value to produce a [[Signal]].
   *   Here, without an initial value, it isn't possible to create a [[Signal]],
   *   as the value before the first event would be unspecified.
   */
  @inline def reduceLeft[B >: A](fn: (B, A) => B): EventStream[B] = {
    scanLeft[Option[B]](None) {
      case (None, next) => Some(next)
      case (Some(prev), next) => Some(fn(prev, next))
    }.updates.collect { case Some(value) => value }
  }

  /**
   * Creates a signal which contains the total number of events emitted by the parent stream so far.
   *
   * @return
   *   A [[Signal]] that counts the number of events, starting from `0`.
   *
   * @see [[EventStream.zipWithIndex]]
   *   for an operator that emits both the event value and its index as a tuple.
   */
  @inline def count: Signal[Long] = {
    scanLeft(0L) { (count, _) => count + 1L }
  }

  /**
   * Creates an event stream that pairs each event from the parent stream with its index (starting from `0`).
   *
   * The 0-based index of an event is equal to the number of events which were triggered before it in the same stream.
   *
   * @return
   *   An [[EventStream]] that emits a tuple of the event value and its index every time that the parent stream emits.
   *
   * @example
   *   {{{
   *     val clicks: EventStream[MouseEvent] = ???
   *     clicks
   *       .zipWithIndex
   *       .tapEach: (click, index) =>
   *         println(s"You've clicked ${index + 1} time(s)!")
   *   }}}
   *
   * @see [[EventStream.count]]
   *   for an operator that counts the number of events, without storing the event values themselves.
   */
  @inline def zipWithIndex: EventStream[(A, Long)] = {
    map(value => (value, 0L)).reduceLeft[(A, Long)] {
      case ((_, index), (value, _)) => (value, index + 1L)
    }
  }

  /**
   * Creates an event stream that emits only the events from the parent stream whose index satisfies the given predicate.
   *
   * The 0-based index of an event is equal to the number of events which were triggered before it in the same stream.
   *
   * @param passes
   *   A predicate that decides inclusion based on the event index.
   *
   * @return
   *   An [[EventStream]] that emits only the events from the parent stream whose index satisfies the given predicate.
   *
   * @example
   *   {{{
   *     val messages = EventStream[String] = ???
   *     val odds = messages.filterIndex(_ % 2 == 0)
   *     val evens = messages.filterIndex(_ % 2 == 1)
   *   }}}
   */
  @inline def filterIndex(passes: Long => Boolean): EventStream[A] = {
    zipWithIndex.collect {
      case (value, index) if passes(index) => value
    }
  }

  /**
   * Creates an event stream that emits only every [[step]]-th event from the parent stream, starting with the first one.
   * In other words, it emits events whose 0-based index is a multiple of [[step]].
   *
   * @param step
   *   The number of events to advance between emitted events.
   *   Note that this is one more than the number of events skipped each cycle.
   *   Must be strictly positive.
   *
   * @param offset
   *   The number of events to skip at the start before emitting the first event.
   *   Must be non-negative (default is `0`, which means that the first event will be included).
   *
   * @return
   *   An [[EventStream]] in which only every [[step]]-th event from the parent stream is emitted, skipping everything else.
   *
   * @throws IllegalArgumentException
   *   if [[step]] is non-positive.
   *
   * @see [[EventStream.filterIndex]]
   *   for a more general operator that can skip based on arbitrary patterns.
   *
   * @see [[EventStream.drop]]
   *   for an operator that skips at the start, but doesn't repeat cyclically.
   */
  @inline def stride(step: Int, offset: Int = 0): EventStream[A] = step match {

    // Error case: step must be strictly positive.
    case step if step <= 0 =>
      throw new IllegalArgumentException(s"stride step must be strictly positive, got $step.")

    // Degenerate case: stride is a no-op if step=1, so we can optimise by doing nothing.
    case 1 => toStream.drop(offset)

    // General case: we can use filterIndex to only include events whose index is a multiple of step.
    case step => toStream.drop(offset).filterIndex(_ % step == 0)
  }

  /**
   * Creates an event stream that emits a sliding window of the last [[size]] events from the parent stream.
   * Each window is represented as a sequence of events, ordered from oldest to newest.
   *
   * @param size
   *   The number of past events to include in each sliding window, including the current event.
   *   In other words, how much history should be remembered?
   *   Must be non-negative.
   *
   * @param step
   *   The number of events to skip between emitted sliding windows.
   *   Must be strictly positive (default is `1`, which includes all windows).
   *
   * @param includeWarmup
   *   The first `size - 1` events in the parent stream will not have enough history to fill the sliding window.
   *   This parameter determines whether these "warmup" events should be included in the resulting stream or not (default is `false`).
   *    - If included, we guarantee that every event in the parent stream will occur in the last position of exactly one sliding window
   *      (provided that [[step]] is `1`).
   *    - If excluded (default), we guarantee that every sliding window will have exactly [[size]] events
   *      (otherwise [[size]] is only an upper bound).
   *
   * @return
   *   An [[EventStream]] that emits a sequence of the last [[size]] events from the parent stream
   *   every time that the parent stream emits an event.
   *
   * @throws IllegalArgumentException
   *   if [[size]] is negative.
   *
   * @note
   *  Exactly one event will be emitted for every event emitted by the parent stream, with the following exceptions:
   *   - If [[includeWarmup]] is `false` (default), the first `size - 1` events will be skipped.
   *   - If [[step]] is greater than `1`, some events will be skipped between emitted sliding windows.
   *
   * @see [[EventStream.grouped]]
   *   for a similar operator that emits non-overlapping groups.
   */
  @inline def sliding(
    size: Int,
    step: Int = 1,
    includeWarmup: Boolean = false,
  ): EventStream[Seq[A]] = size match {

    // Error case: size must be non-negative.
    case size if size < 0 =>
      throw new IllegalArgumentException(s"sliding size must be non-negative, got $size.")

    // Degenerate cases: we can optimise these by skipping the scanLeft and just mapping the parent stream.
    case 0 => mapTo(Seq.empty)
    case 1 => map(Seq(_))

    // General case: we need to use scanLeft to keep track of the last `size` events.
    case size =>
      scanLeft(Seq.empty[A]) {
        (current, next) => (current :+ next).takeRight(size)
      }
        .updates
        .drop(if (includeWarmup) 0 else size - 1)
        .stride(step)
  }

  /**
   * Creates an event stream that tags each event from the parent stream with the value of the previous event,
   * forming pairs of consecutive events. The first event from the parent stream will not be emitted,
   * as it does not have a previous event to pair with.
   *
   * @return
   *   An [[EventStream]] that emits a tuple of the last two events,
   *   with the older event on the left and the newer event on the right.
   *
   * @note
   *   Equivalent to: {{{
   *     sliding(size=2, step=1, includeWarmup=false).map { case Seq(a, b) => (a, b) }
   *   }}}
   *
   * @see [[EventStream.sliding]]
   *   for a more general operator that emits sliding windows of arbitrary size.
   */
  @inline def pairs: EventStream[(A, A)] = {
    sliding(2).map { case Seq(a, b) => (a, b) }
  }

  /**
   * Creates an event stream that emits non-overlapping contiguous groups of events from the parent stream.
   * Each group is represented as a sequence of events, ordered from oldest to newest.
   *
   * @param size
   *   The size (number of events) of each group. Must be strictly positive.
   *
   * @return
   *   An [[EventStream]] that emits non-overlapping contiguous groups of events from the parent stream.
   *
   * @note
   *   Equivalent to: {{{
   *     sliding(size=size, step=size, includeWarmup=false)
   *   }}}
   *
   * @see [[EventStream.sliding]]
   *   for a more general operator that emits overlapping sliding windows.
   */
  @inline def grouped(size: Int): EventStream[Seq[A]] = {
    sliding(size = size, step = size, includeWarmup = false)
  }

  @deprecated("foldLeft was renamed to scanLeft", "15.0.0-M1")
  def foldLeft[B](initial: B)(fn: (B, A) => B): Signal[B] = scanLeft(initial)(fn)

  @deprecated("foldLeftRecover was renamed to scanLeftRecover", "15.0.0-M1")
  def foldLeftRecover[B](initial: Try[B])(fn: (Try[B], Try[A]) => Try[B]): Signal[B] = scanLeftRecover(initial)(fn)
}
