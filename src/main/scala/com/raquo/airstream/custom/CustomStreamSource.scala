package com.raquo.airstream.custom

import com.raquo.airstream.core.{EventStream, Transaction, WritableEventStream}
import com.raquo.airstream.custom.CustomSource._

/** Use this to easily create a custom signal from an external source
  *
  * See docs on custom sources, and [[CustomSource.Config]]
  */
class CustomStreamSource[A] private (
  makeConfig: (FireValue[A], FireError, GetStartIndex, GetIsStarted) => CustomSource.Config,
) extends WritableEventStream[A] with CustomSource[A] {

  override protected[this] val config: Config = makeConfig(
    value => {
      new Transaction(fireValue(value, _))
    },
    err => {
      new Transaction(fireError(err, _))
    },
    () => startIndex,
    () => isStarted
  )
}

object CustomStreamSource {

  def apply[A](
    config: (FireValue[A], FireError, GetStartIndex, GetIsStarted) => Config
  ): EventStream[A] = {
    new CustomStreamSource[A](config)
  }
}
