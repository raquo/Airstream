package com.raquo.airstream.flatten

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.ownership.ManualOwner
import com.raquo.airstream.state.Val

import scala.collection.mutable

final class ConcurrentStreamRegressionSpec extends UnitSpec {

  it("flattenMerge starts the initial inner stream supplied by a signal") {
    val owner = ManualOwner()
    val received = mutable.Buffer.empty[Int]
    val stream = Val(EventStream.fromValue(42)).flattenMerge

    try {
      stream.foreach { value => received += value }(owner)

      // fromValue emits synchronously once the shared start transaction finishes.
      assert(received.toList == List(42))
    } finally {
      owner.killSubscriptions()
    }
  }
}
