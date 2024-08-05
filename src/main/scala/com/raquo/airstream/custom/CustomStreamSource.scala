package com.raquo.airstream.custom

import com.raquo.airstream.core.{EventStream, Transaction, WritableStream}
import com.raquo.airstream.custom.CustomSource._

/** Use this to easily create a custom signal from an external source
  *
  * See docs on custom sources, and [[CustomSource.Config]]
  */
class CustomStreamSource[A](
    makeConfig: (
        FireValue[A],
        FireError,
        GetStartIndex,
        GetIsStarted
    ) => CustomSource.Config
) extends WritableStream[A]
    with CustomSource[A] {

  override protected[this] val config: Config = makeConfig(
    value => Transaction(fireValue(value, _)),
    err => Transaction(fireError(err, _)),
    () => startIndex,
    () => isStarted
  )
}

object CustomStreamSource {

  @deprecated("Use EventStream.fromCustomSource", "15.0.0-M1")
  def apply[A](
      config: (FireValue[A], FireError, GetStartIndex, GetIsStarted) => Config
  ): EventStream[A] = {
    new CustomStreamSource[A](config)
  }
}
