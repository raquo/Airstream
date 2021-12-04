package com.raquo.airstream.custom

import com.raquo.airstream.core.{Signal, Transaction, WritableSignal}
import com.raquo.airstream.custom.CustomSource._

import scala.util.{Success, Try}

// @TODO[Test] needs testing

/** Use this to easily create a custom signal from an external source
  *
  * See docs on custom sources, and [[CustomSource.Config]]
  *
  * @param cacheInitialValue if false, this signal's initial value will be re-evaluated on every
  *                          restart (so long as it does not emit any values)
  */
class CustomSignalSource[A] (
  getInitialValue: => Try[A],
  makeConfig: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => CustomSource.Config,
  cacheInitialValue: Boolean = false
) extends WritableSignal[A] with CustomSource[A] {

  private var hasEmittedEvents = false

  override protected[this] val config: Config = makeConfig(
    value => {
      hasEmittedEvents = true
      new Transaction(fireTry(value, _))
    },
    tryNow,
    () => startIndex,
    () => isStarted
  )

  override protected def currentValueFromParent(): Try[A] = {
    // #Note See also SignalFromEventStream for similar logic
    // #Note This can be called from inside tryNow(), so make sure to avoid an infinite loop
    if (maybeLastSeenCurrentValue.nonEmpty && (hasEmittedEvents || cacheInitialValue)) {
      tryNow()
    } else {
      getInitialValue
    }
  }
}

object CustomSignalSource {

  def apply[A](
    initial: => A,
    cacheInitialValue: Boolean = false
  )(
    config: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => Config
  ): Signal[A] = {
    new CustomSignalSource[A](Success(initial), config, cacheInitialValue)
  }

  def fromTry[A](
    initial: => Try[A],
    cacheInitialValue: Boolean = false
  )(
    config: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => Config
  ): Signal[A] = {
    new CustomSignalSource[A](initial, config, cacheInitialValue)
  }
}
