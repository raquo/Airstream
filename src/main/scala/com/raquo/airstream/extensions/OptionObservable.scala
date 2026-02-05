package com.raquo.airstream.extensions

import com.raquo.airstream.core.{BaseObservable, EventStream, Observable, Signal}
import com.raquo.airstream.split.DuplicateKeysConfig
import com.raquo.airstream.state.StrictSignal

/** See also: [[OptionStream]] for stream-specific operators */
class OptionObservable[A, Self[+_] <: Observable[_]](
  val observable: BaseObservable[Self, Option[A]]
) extends AnyVal {

  /** Maps the value in Some(x) */
  def mapSome[B](project: A => B): Self[Option[B]] = {
    observable.map(_.map(project))
  }

  /** Filters the value in Some(x) */
  def mapFilterSome(passes: A => Boolean): Self[Option[A]] = {
    observable.map(_.filter(passes))
  }

  /** Maps the value in Some(x), and the None value, to a common type. */
  def foldOption[B](ifEmpty: => B)(some: A => B): Self[B] = {
    observable.map(_.fold(ifEmpty)(some))
  }

  /** Maps Option[A] to Either[L, A] - you need to provide the L. */
  def mapToRight[L](left: => L): Self[Either[L, A]] = {
    observable.map(_.toRight(left))
  }

  /** Maps Option[A] to Either[A, R] - you need to provide the R. */
  def mapToLeft[R](right: => R): Self[Either[A, R]] = {
    observable.map(_.toLeft(right))
  }

  /** This `.split`-s an Observable of an Option by the Option's `isDefined` property.
    * If you want a different key, use the .split operator directly.
    * If the observable is a stream, it's treated as if it contains None before it emits its first event.
    *
    * @param project - signalOfInput => output
    *
    *                  `project` is called whenever the parent observable switches from `None` to `Some(value)`.
    *                  `signalOfInput` starts with an initial `Some(value)`, and updates whenever
    *                  the parent observable updates from `Some(a)` to `Some(b)`.
    *
    *                  You can get the signal's current value with `.now()`.
    *
    * @param ifEmpty - returned if Option is empty, or if the parent observable is a stream and has not emitted
    *                  any events yet. Re-evaluated whenever the parent observable switches from
    *                  `Some(a)` to `None`. `ifEmpty` is NOT re-evaluated when the parent
    *                  observable emits `None` if the last event it emitted was also a `None`.
    */
  def splitOption[B](
    project: StrictSignal[A] => B,
    ifEmpty: => B
  ): Signal[B] = {
    val signal = observable.toSignalIfStream(
      _.startWith(initial = None, cacheInitialValue = true)
    )
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    signal
      .distinctByFn((prev, next) => prev.isEmpty && next.isEmpty) // Ignore consecutive `None` events
      .splitSeq(
        key = _ => (),
        duplicateKeys = DuplicateKeysConfig.noWarnings
      )(project)
      .map(_.getOrElse(ifEmpty))
  }

  def splitOption[B](
    project: StrictSignal[A] => B
  ): Signal[Option[B]] = {
    splitOption(
      signal => Some(project(signal)),
      ifEmpty = None
    )
  }
}
