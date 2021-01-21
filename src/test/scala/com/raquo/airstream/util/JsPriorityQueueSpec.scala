package com.raquo.airstream.util

import com.raquo.airstream.UnitSpec

import scala.util.Try

class JsPriorityQueueSpec extends UnitSpec {

  class Foo(val rank: Int) {
    override def toString: String = s"Foo@${this.hashCode().toHexString}(rank=$rank)"
  }

  it("queues items by priority") {

    val q = new JsPriorityQueue[String](_.length)

    // --

    q.enqueue("22")
    q.enqueue("333")
    q.enqueue("1")
    q.enqueue("4444")

    assert(q.debugQueue == List(
      "1", "22", "333", "4444"
    ))

    // --

    assert(q.dequeue() == "1")

    assert(q.debugQueue == List(
      "22", "333", "4444"
    ))

    // --

    assert(q.dequeue() == "22")

    assert(q.debugQueue == List(
      "333", "4444"
    ))

    // --

    assert(q.dequeue() == "333")

    assert(q.debugQueue == List(
      "4444"
    ))

    // --

    assert(q.dequeue() == "4444")

    assert(q.debugQueue == Nil)

    // --

    assert(Try(q.dequeue()).isFailure)

    // --

    q.enqueue("a")
    q.enqueue("bb")

    assert(q.debugQueue == List(
      "a", "bb"
    ))

    // --

    assert(q.dequeue() == "a")

    assert(q.debugQueue == List(
      "bb"
    ))

    // --

    q.enqueue("dddd")

    assert(q.debugQueue == List(
      "bb", "dddd"
    ))

    // --

    q.enqueue("ccc")

    assert(q.debugQueue == List(
      "bb", "ccc", "dddd"
    ))

    // --

    q.enqueue("a")

    assert(q.debugQueue == List(
      "a", "bb", "ccc", "dddd"
    ))

    // --

    assert(q.dequeue() == "a")

    assert(q.debugQueue == List(
      "bb", "ccc", "dddd"
    ))

    // --

    assert(q.dequeue() == "bb")

    assert(q.debugQueue == List(
      "ccc", "dddd"
    ))

    // --

    assert(q.dequeue() == "ccc")

    assert(q.debugQueue == List(
      "dddd"
    ))

    // --

    assert(q.dequeue() == "dddd")

    assert(q.debugQueue == Nil)
  }

  it("same-priority edge cases") {

    val q = new JsPriorityQueue[Foo](_.rank)

    val f1 = new Foo(1)
    val f2 = new Foo(2)
    val f31 = new Foo(3)
    val f32 = new Foo(3)
    val f33 = new Foo(3)
    val f41 = new Foo(4)
    val f42 = new Foo(4)
    val f5 = new Foo(5)

    // --

    q.enqueue(f5)
    q.enqueue(f41)
    q.enqueue(f42)
    q.enqueue(f33)
    q.enqueue(f31)
    q.enqueue(f32)
    q.enqueue(f1)
    q.enqueue(f2)

    assert(q.debugQueue == List(
      f1, f2, f33, f31, f32, f41, f42, f5
    ))

    // --

    assert(q.dequeue() == f1)
    assert(q.dequeue() == f2)

    assert(q.debugQueue == List(
      f33, f31, f32, f41, f42, f5
    ))

    // --

    assert(q.dequeue() == f33)

    assert(q.debugQueue == List(
      f31, f32, f41, f42, f5
    ))

    // --

    assert(q.dequeue() == f31)

    assert(q.debugQueue == List(
      f32, f41, f42, f5
    ))

    // --

    assert(q.dequeue() == f32)

    assert(q.debugQueue == List(
      f41, f42, f5
    ))

    // --

    assert(q.dequeue() == f41)

    assert(q.debugQueue == List(
      f42, f5
    ))

    // --

    assert(q.dequeue() == f42)

    assert(q.debugQueue == List(
      f5
    ))

    // --

    assert(q.dequeue() == f5)

    assert(q.debugQueue == Nil)
  }

  it("duplicate items edge case") {

    val q = new JsPriorityQueue[Foo](_.rank)

    val f1 = new Foo(1)
    val f2 = new Foo(2)
    val f3 = new Foo(3)
    val f4 = new Foo(4)

    // --

    q.enqueue(f2)
    q.enqueue(f1)
    q.enqueue(f1)
    q.enqueue(f4)
    q.enqueue(f3)
    q.enqueue(f2)

    assert(q.debugQueue == List(
      f1, f1, f2, f2, f3, f4
    ))

    // --

    assert(q.dequeue() == f1)

    assert(q.debugQueue == List(
      f1, f2, f2, f3, f4
    ))

    // --

    assert(q.dequeue() == f1)

    assert(q.debugQueue == List(
      f2, f2, f3, f4
    ))

    // --

    assert(q.dequeue() == f2)

    assert(q.debugQueue == List(
      f2, f3, f4
    ))

    // --

    assert(q.dequeue() == f2)

    assert(q.debugQueue == List(
      f3, f4
    ))

    // --

    assert(q.dequeue() == f3)

    assert(q.debugQueue == List(
      f4
    ))

    // --

    assert(q.dequeue() == f4)

    assert(q.debugQueue == Nil)
  }
}
