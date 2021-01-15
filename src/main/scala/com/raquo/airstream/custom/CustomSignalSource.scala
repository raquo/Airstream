package com.raquo.airstream.custom

import com.raquo.airstream.core.{ Signal, WritableSignal }
import com.raquo.airstream.custom.CustomSource._

import scala.util.{ Success, Try }

// @TODO[Test] needs testing

/** Use this to easily create a custom signal from an external source
  *
  * See docs on custom sources, and [[CustomSource.Config]]
  */
class CustomSignalSource[A] (
  getInitialValue: => Try[A],
  makeConfig: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => CustomSource.Config,
) extends Signal[A] with WritableSignal[A] with CustomSource[A] {

  override protected def initialValue: Try[A] = getInitialValue

  override protected val config: Config = makeConfig(_fireTry, tryNow _, getStartIndex, getIsStarted)
}

object CustomSignalSource {

  def apply[A](
    initial: => A
  )(
    config: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => Config
  ): Signal[A] = {
    new CustomSignalSource[A](Success(initial), config)
  }

  def fromTry[A](
    initial: => Try[A]
  )(
    config: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => Config
  ): Signal[A] = {
    new CustomSignalSource[A](initial, config)
  }
}
