package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}

/** See also [[BooleanObservable]] for generic boolean operators */
class BooleanStream(
  private val stream: EventStream[Boolean]
) extends AnyVal {

  // #nc[split] remove this if we don't come up with operators to put here
}
