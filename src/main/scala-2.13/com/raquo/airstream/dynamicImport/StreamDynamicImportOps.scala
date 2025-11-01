package com.raquo.airstream.dynamicImport

import com.raquo.airstream.core.EventStream

trait StreamDynamicImportOps[+A] { this: EventStream[A] =>

  // `dynamicImport` operator is only for Scala 3 because it relies on `inline`.
}
