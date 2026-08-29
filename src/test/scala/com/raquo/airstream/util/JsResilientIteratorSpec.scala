package com.raquo.airstream.util

import com.raquo.airstream.UnitSpec

import scala.collection.mutable
import scala.util.Try

/** Direct tests for the fragile cursor bookkeeping in [[JsResilientIterator]].
  *
  * The owner specs exercise this class end-to-end; these tests pin the mechanical
  * contract (visit order, cursor adjustment on prepend/remove, appended-item
  * handling in both iteration modes) in isolation.
  */
class JsResilientIteratorSpec extends UnitSpec {

  private def newIter(initial: Int*): JsResilientIterator[Int] = {
    val iter = new JsResilientIterator[Int]
    initial.foreach(iter.append)
    iter
  }

  // -- forEachExisting: does NOT visit appended items --

  it("forEachExisting - baseline visits all present items once, in order") {
    val iter = newIter(1, 2, 3)
    val visited = mutable.Buffer[Int]()
    iter.forEachExisting(visited.append(_))
    visited.toList shouldBe List(1, 2, 3)
  }

  it("forEachExisting - item appended mid-pass is NOT visited") {
    val iter = newIter(1, 2, 3)
    val visited = mutable.Buffer[Int]()
    iter.forEachExisting { x =>
      visited.append(x)
      if (x == 1) iter.append(99)
    }
    visited.toList shouldBe List(1, 2, 3)
    iter.toList shouldBe List(1, 2, 3, 99) // 99 is present, just not visited
  }

  // -- forEachExistingAndAppended: DOES visit appended items --

  it("forEachExistingAndAppended - item appended mid-pass IS visited, after existing items") {
    val iter = newIter(1, 2, 3)
    val visited = mutable.Buffer[Int]()
    iter.forEachExistingAndAppended { x =>
      visited.append(x)
      if (x == 1) iter.append(99)
    }
    visited.toList shouldBe List(1, 2, 3, 99)
  }

  it("forEachExistingAndAppended - transitively appended items are all visited (unbounded growth handled)") {
    val iter = newIter(1)
    val visited = mutable.Buffer[Int]()
    iter.forEachExistingAndAppended { x =>
      visited.append(x)
      if (x < 3) iter.append(x + 1) // each visit appends the next, until 3
    }
    visited.toList shouldBe List(1, 2, 3)
  }

  it("forEachExistingAndAppended - append interleaved with remove keeps every live item exactly once") {
    val iter = newIter(1, 2, 3)
    val visited = mutable.Buffer[Int]()
    iter.forEachExistingAndAppended { x =>
      visited.append(x)
      if (x == 1) { val _ = iter.append(4) }
      if (x == 2) { val _ = iter.remove(1) } // remove an already-visited item
      if (x == 3) { val _ = iter.append(5) }
    }
    visited.toList shouldBe List(1, 2, 3, 4, 5)
    iter.toList shouldBe List(2, 3, 4, 5)
  }

  it("forEachExistingAndAppended - prepended item is still NOT visited") {
    val iter = newIter(1, 2, 3)
    val visited = mutable.Buffer[Int]()
    iter.forEachExistingAndAppended { x =>
      visited.append(x)
      if (x == 1) iter.prepend(0)
    }
    visited.toList shouldBe List(1, 2, 3)
    iter.toList shouldBe List(0, 1, 2, 3)
  }

  // -- remove during iteration: cursor safety --

  it("remove - removing a not-yet-reached item skips it and never reads out of bounds") {
    val iter = newIter(1, 2, 3, 4)
    val visited = mutable.Buffer[Int]()
    iter.forEachExisting { x =>
      visited.append(x)
      if (x == 1) { val _ = iter.remove(3) } // remove an item ahead of the cursor
    }
    visited.toList shouldBe List(1, 2, 4)
  }

  it("remove - removing an already-visited item does not skip the following item") {
    val iter = newIter(1, 2, 3, 4)
    val visited = mutable.Buffer[Int]()
    iter.forEachExisting { x =>
      visited.append(x)
      if (x == 3) { val _ = iter.remove(1) } // remove an item behind the cursor
    }
    visited.toList shouldBe List(1, 2, 3, 4)
  }

  it("remove - removing the current item does not skip the next item") {
    val iter = newIter(1, 2, 3, 4)
    val visited = mutable.Buffer[Int]()
    iter.forEachExisting { x =>
      visited.append(x)
      if (x == 2) { val _ = iter.remove(2) } // remove self mid-visit
    }
    visited.toList shouldBe List(1, 2, 3, 4)
    iter.toList shouldBe List(1, 3, 4)
  }

  it("remove - returns false for an absent item and leaves the list unchanged") {
    val iter = newIter(1, 2, 3)
    iter.remove(99) shouldBe false
    iter.toList shouldBe List(1, 2, 3)
  }

  // -- prepend during iteration --

  it("prepend - prepended item is not visited and the pass keeps visiting the same logical items") {
    val iter = newIter(1, 2, 3)
    val visited = mutable.Buffer[Int]()
    iter.forEachExisting { x =>
      visited.append(x)
      if (x == 1) iter.prepend(0)
    }
    visited.toList shouldBe List(1, 2, 3)
    iter.toList shouldBe List(0, 1, 2, 3)
  }

  // -- guards --

  it("forEachExisting is not re-entrant") {
    val iter = newIter(1, 2, 3)
    val result = Try {
      iter.forEachExisting { _ =>
        iter.forEachExisting { _ => () } // nested pass on the same instance
      }
    }
    result.isFailure shouldBe true
    result.failed.get.getMessage.contains("already iterating") shouldBe true
  }

  it("clear during iteration throws") {
    val iter = newIter(1, 2, 3)
    val result = Try {
      iter.forEachExisting { _ => iter.clear() }
    }
    result.isFailure shouldBe true
    result.failed.get.getMessage.contains("during iteration") shouldBe true
  }

  it("the cursor is reset after the callback throws, so a later pass works normally") {
    val iter = newIter(1, 2, 3)
    val _ = Try {
      iter.forEachExisting { x => if (x == 2) throw new Exception("boom") }
    }
    // If the cursor were left dangling, this second pass would misbehave / read OOB.
    val visited = mutable.Buffer[Int]()
    iter.forEachExisting(visited.append(_))
    visited.toList shouldBe List(1, 2, 3)
  }
}
