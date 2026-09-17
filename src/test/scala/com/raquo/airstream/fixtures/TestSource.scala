package com.raquo.airstream.fixtures

import com.raquo.airstream.core.{EventStream, Signal}

import scala.collection.mutable
import scala.util.Try

/** Builders for instrumented custom-source observables, for lifecycle tests.
  *
  * Each built observable logs `Effect("<label>-start", "ix-<n>")` when it starts,
  * and `Effect("<label>-stop", "ix-<n>")` when it stops, into the shared `effects`
  * buffer (`<n>` is the start index). This lets a test assert start/stop of an inner
  * observable directly, instead of inferring it from downstream calculations.
  *
  * The optional `onStart` callback receives the observable's value emitter
  * (`fireValue` for a stream, `setCurrValue` for a signal), so a test can capture it
  * (e.g. `onStart = updateA = _`) to drive the source later. `onStop` runs any custom
  * teardown code. Both default to no-ops.
  */
object TestSource {

  def stream[A](
    effects: mutable.Buffer[Effect[?]],
    label: String,
    onStart: (A => Unit) => Unit = (_: A => Unit) => (),
    onStop: () => Unit = () => ()
  ): EventStream[A] = {
    EventStream.fromCustomSource[A](
      start = (fireValue, _, getStartIndex, _) => {
        onStart(fireValue)
        effects += Effect(s"$label-start", "ix-" + getStartIndex())
      },
      stop = startIndex => {
        onStop()
        effects += Effect(s"$label-stop", "ix-" + startIndex)
      }
    )
  }

  def signal[A](
    effects: mutable.Buffer[Effect[?]],
    label: String,
    initial: => Try[A],
    onStart: (Try[A] => Unit) => Unit = (_: Try[A] => Unit) => (),
    onStop: () => Unit = () => ()
  ): Signal[A] = {
    Signal.fromCustomSource[A](
      initial = initial,
      start = (setCurrValue, _, getStartIndex, _) => {
        onStart(setCurrValue)
        effects += Effect(s"$label-start", "ix-" + getStartIndex())
      },
      stop = startIndex => {
        onStop()
        effects += Effect(s"$label-stop", "ix-" + startIndex)
      }
    )
  }
}
