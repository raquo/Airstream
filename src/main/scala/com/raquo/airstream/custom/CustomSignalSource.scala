package com.raquo.airstream.custom

import com.raquo.airstream.core.{Signal, Transaction, WritableSignal}
import com.raquo.airstream.custom.CustomSource._

import scala.util.{Success, Try}

// @TODO[Test] needs testing

/** Use this to easily create a custom signal from an external source
  *
  * See docs on custom sources, and [[CustomSource.Config]]
  */
class CustomSignalSource[A] (
  getInitialValue: => Try[A],
  makeConfig: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => CustomSource.Config
) extends WritableSignal[A] with CustomSource[A] {

  override protected[this] val config: Config = makeConfig(
    value => new Transaction(fireTry(value, _)),
    () => tryNow(),
    () => startIndex,
    () => isStarted
  )

  override protected def currentValueFromParent(): Try[A] = getInitialValue
}

object CustomSignalSource {

  @deprecated("Use Signal.fromCustomSource", "15.0.0-M1")
  def apply[A](
    initial: => A
  )(
    config: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => Config
  ): Signal[A] = {
    new CustomSignalSource[A](Success(initial), config)
  }

  @deprecated("Use Signal.fromCustomSourceTry", "15.0.0-M1")
  def fromTry[A](
    initial: => Try[A]
  )(
    config: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => Config
  ): Signal[A] = {
    new CustomSignalSource[A](initial, config)
  }
}
