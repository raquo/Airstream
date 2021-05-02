package com.raquo.airstream.custom

import com.raquo.airstream.core.{ Transaction, WritableObservable }
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


  protected[this] val _fireValue: FireValue[A] = { value =>
    //println(s"> init trx from CustomSource(${value})")
    new Transaction(fireValue(value, _))
  }

  protected[this] val _fireError: FireError = { error =>
    //println(s"> init error trx from CustomSource(${error})")
    new Transaction(fireError(error, _))
  }

  protected[this] val _fireTry: SetCurrentValue[A] = { value =>
    //println(s"> init try trx from CustomSource(${value})")
    new Transaction(fireTry(value, _))
  }

  protected[this] val getStartIndex: GetStartIndex = () => startIndex

  protected[this] val getIsStarted: GetIsStarted = () => isStarted

  override protected[this] def onStart(): Unit = {
    startIndex += 1
    Try(config.onStart()).recover {
      case err: Throwable => _fireError(err)
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
    val onStart: () => Unit,
    val onStop: () => Unit
  ) {

    /** Create a version of a config that only runs start / stop if the predicate passes.
      * - `start` will be run when the CustomSource is about to start
      *   if `passes` returns true at that time
      * - `stop` will be run when the CustomSource is about to stop
      *   if your `start` code ran the last time CustomSource started
      */
    def when(passes: () => Boolean): Config = {
      var started = false
      new Config(
        () => {
          if (passes()) {
            started = true
            onStart()
          }
        },
        onStop = () => {
          if (started) {
            onStop()
          }
          started = false
        }
      )
    }
  }

  object Config {

    def apply(onStart: () => Unit, onStop: () => Unit): Config = {
      new Config(onStart, onStop)
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
