package com.raquo.airstream.extensions

import com.raquo.airstream.core.Signal
import com.raquo.airstream.split.DuplicateKeysConfig
import com.raquo.airstream.state.{LazyDerivedVar, LazyStrictSignal, Var}

class OptionVar[A](val v: Var[Option[A]]) extends AnyVal {

  /** This `.split`-s a Var of an Option by the Option's `isDefined` property.
    * If you want a different key, use the .split operator directly.
    *
    * @param project - (initialInput, varOfInput) => output
    *                  `project` is called whenever the parent var switches from `None` to `Some()`.
    *                  `varOfInput` starts with `initialInput` value, and updates when
    *                  the parent var updates from `Some(a)` to `Some(b)`.
    * @param ifEmpty - returned if Option is empty. Evaluated whenever the parent var
    *                  switches from `Some(a)` to `None`, or when the parent var
    *                  starts with a `None`. `ifEmpty` is NOT re-evaluated when the
    *                  parent var emits `None` if its value is already `None`.
    */
  def splitOption[B](
    project: (A, Var[A]) => B,
    ifEmpty: => B
  ): Signal[B] = {
    // Note: We never have duplicate keys here, so we can use
    // DuplicateKeysConfig.noWarnings to improve performance
    v.signal
      .distinctByFn((prev, next) => prev.isEmpty && next.isEmpty) // Ignore consecutive `None` events
      .split(
        key = _ => (),
        duplicateKeys = DuplicateKeysConfig.noWarnings
      ) { (_, initial, signal) =>
        val displayNameSuffix = s".splitOption(Some)"
        val childVar = new LazyDerivedVar[Option[A], A](
          parent = v,
          signal = new LazyStrictSignal[A, A](
            signal, identity, signal.displayName, displayNameSuffix + ".signal"
          ),
          zoomOut = (inputs, newInput) => {
            Some(newInput)
          },
          displayNameSuffix = displayNameSuffix
        )
        project(initial, childVar)
      }
      .map(_.getOrElse(ifEmpty))
  }

  def splitOption[B](
    project: (A, Var[A]) => B
  ): Signal[Option[B]] = {
    splitOption(
      (initial, someVar) => Some(project(initial, someVar)),
      ifEmpty = None
    )
  }
}
