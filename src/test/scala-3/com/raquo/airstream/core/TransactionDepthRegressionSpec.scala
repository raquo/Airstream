package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec

import scala.collection.mutable

final class TransactionDepthRegressionSpec extends UnitSpec {

  it("maxDepth = -1 disables the depth limit and permits nested transactions") {
    val previousMaxDepth = Transaction.maxDepth
    val errors = mutable.Buffer.empty[Throwable]
    val received = mutable.Buffer.empty[Int]
    val errorCallback: Throwable => Unit = error => {
      errors += error
      ()
    }

    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    try {
      Transaction.maxDepth = -1
      Transaction { _ =>
        received += 1
        Transaction { _ => received += 2 }
      }

      assert(received.toList == List(1, 2), s"Transactions were rejected: $errors")
      assert(errors.isEmpty)
    } finally {
      Transaction.maxDepth = previousMaxDepth
      AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
      AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    }
  }
}
