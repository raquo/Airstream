package com.raquo.airstream.syntax

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{EventStream, Observable, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.TestableOwner
import com.raquo.airstream.state.Var
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.annotation.nowarn
import scala.concurrent.Future

class Scala3SyntaxSpec extends UnitSpec {

  it("22-tuple methods (Scala 3 native)") {
    // format: off
    val owner = new TestableOwner
    withClue("[stream]") {
      EventStream
        .fromValue((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22))
        .map { (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22) =>
          (
            v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9 + v10 + v11,
            v12 + v13 + v14 + v15 + v16 + v17 + v18 + v19 + v20 + v21 + v22
          )
        }
        .foreach { case (part1, part2) =>
          assertEquals(part1, 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10 + 11)
          assertEquals(part2, 12 + 13 + 14 + 15 + 16 + 17 + 18 + 19 + 20 + 21 + 22)

        }(owner)
    }
    withClue("[signal]") {
      Signal
        .fromValue((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22))
        .map { (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22) =>
          (
            v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9 + v10 + v11,
            v12 + v13 + v14 + v15 + v16 + v17 + v18 + v19 + v20 + v21 + v22
          )
        }
        .foreach { case (part1, part2) =>
          assertEquals(part1, 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10 + 11)
          assertEquals(part2, 12 + 13 + 14 + 15 + 16 + 17 + 18 + 19 + 20 + 21 + 22)

        }(owner)
    }
    // format: on
  }
}
