package com.raquo.airstream.custom

import com.raquo.airstream.core.{Transaction, WritableObservable}
import com.raquo.airstream.custom.CustomSource._

import scala.util.Try

// @TODO[Docs] Write docs and link to that.

/** Base functionality for a custom observable based on start and stop callbacks.
  *
  * See:
  * - [[com.raquo.airstream.custom.CustomStreamSource]]
  * - [[com.raquo.airstream.custom.CustomSignalSource]]
  */
trait CustomSource[A] extends WritableObservable[A] {

  protected[this] val config: Config

  // --

  /** CustomSource is intended for observables that don't synchronously depend on other observables. */
  override protected val topoRank: Int = 1

  protected[this] var startIndex: StartIndex = 0

  override protected def onWillStart(): Unit = {
    startIndex += 1
    config.onWillStart()
  }

  override protected[this] def onStart(): Unit = {
    Try(config.onStart()).recover[Unit] {
      case err: Throwable => new Transaction(fireError(err, _))
    }
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    config.onStop()
    super.onStop()
  }
}

object CustomSource {

  /** See docs for custom sources */
  final class Config private (
    val onWillStart: () => Unit,
    val onStart: () => Unit,
    val onStop: () => Unit
  ) { self =>

    /** Create a version of a config that only runs start / stop if the predicate passes.
      * - `start` will be run when the CustomSource is about to start
      *   if `passes` returns true at that time
      * - `stop` will be run when the CustomSource is about to stop
      *   if your `start` code ran the last time CustomSource started
      */
    def when(passes: () => Boolean): Config = {
      var passed = false
      new Config(
        onWillStart = () => {
          passed = passes()
          if (passed) {
            self.onWillStart()
          }
        },
        onStart = () => {
          if (passed) {
            self.onStart()
          }
        },
        onStop = () => {
          if (passed) {
            self.onStop()
          }
          passed = false
        }
      )
    }
  }

  object Config {

    def apply(
      onWillStart: () => Unit,
      onStart: () => Unit,
      onStop: () => Unit
    ): Config = {
      new Config(onWillStart, onStart, onStop)
    }

    def apply(
      onStart: () => Unit,
      onStop: () => Unit
    ): Config = {
      new Config(onWillStart = () => (), onStart, onStop)
    }
  }

  type StartIndex = Int

  type FireValue[A] = A => Unit

  type FireError = Throwable => Unit

  type SetCurrentValue[A] = Try[A] => Unit

  type GetCurrentValue[A] = () => Try[A]

  type GetStartIndex = () => StartIndex

  type GetIsStarted = () => Boolean
}
