package com.raquo.airstream.dynamicImport

import com.raquo.airstream.core.Signal

trait SignalDynamicImportOps[+A] { this: Signal[A] =>

  // `dynamicImport` operator is only for Scala 3 because it relies on `inline`.
}
