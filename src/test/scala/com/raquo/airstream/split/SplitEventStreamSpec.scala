package com.raquo.airstream.split

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.util.Id
import com.raquo.airstream.state.Var

import scala.collection.mutable

class SplitEventStreamSpec extends UnitSpec {

  case class Foo(id: String, version: Int)

  case class Bar(id: String)

  it("splits stream into streams") {

    val effects = mutable.Buffer[Effect[String]]()

    val bus = new EventBus[List[Foo]]

    val owner = new TestableOwner

    val stream = bus.events.split(_.id)((key, initialFoo, fooStream) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + initialFoo.version.toString)
      fooStream.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    stream.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldEqual mutable.Buffer()

    // --

    bus.writer.onNext(Foo("a", 1) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("result", "List(Bar(a))"),
      Effect("update-child", "a-1")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(a))"), // this is a stream, not a signal, so it still emits this
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 3) :: Foo("b", 1) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("result", "List(Bar(a), Bar(b))"),
      Effect("update-child", "a-3"),
      Effect("update-child", "b-1")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))"),
      // this is a stream, not a signal, so it still emits these
      Effect("update-child", "a-3"),
      Effect("update-child", "b-1")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(b))"),
      Effect("update-child", "b-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      // this is a stream, not a signal, so it still emits these
      Effect("result", "List(Bar(b))"),
      Effect("update-child", "b-2")
    )

    effects.clear()

  }

  it("splits stream into signals") {

    val effects = mutable.Buffer[Effect[String]]()

    val bus = new EventBus[List[Foo]]

    val owner = new TestableOwner

    val stream = bus.events.splitIntoSignals(_.id)(project = (key, initialFoo, fooStream) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + initialFoo.version.toString)
      fooStream.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    stream.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldEqual mutable.Buffer()

    // --

    bus.writer.onNext(Foo("a", 1) :: Nil)

    // @TODO[Integrity] This is slightly different order than SplitEventStream.
    //  Not sure if this will be a problem. I think I would prefer if `result` emitted *before* `update-child-stream`
    effects shouldEqual mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("update-child", "a-1"),
      Effect("result", "List(Bar(a))")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(a))"), // output is a stream, not signal
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 3) :: Foo("b", 1) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("update-child", "b-1"),
      Effect("result", "List(Bar(a), Bar(b))"),
      Effect("update-child", "a-3")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(b))"),
      Effect("update-child", "b-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(b))") // output is a stream, not signal
    )

  }

  it("split signal into signals") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

    val owner = new TestableOwner

    val signal = myVar.signal.split(_.id)(project = (key, initialFoo, fooStream) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + initialFoo.version.toString)
      fooStream.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    signal.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldEqual mutable.Buffer(
      Effect("init-child", "initial-1"),
      Effect("update-child", "initial-1"),
      Effect("result", "List(Bar(initial))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 1) :: Nil)

    // @TODO[Integrity] This is slightly different order than SplitEventStream.
    //  Not sure if this will be a problem. I think I would prefer if `result` emitted *before* `update-child-stream`
    effects shouldEqual mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("update-child", "a-1"),
      Effect("result", "List(Bar(a))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 3) :: Foo("b", 1) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("update-child", "b-1"),
      Effect("result", "List(Bar(a), Bar(b))"),
      Effect("update-child", "a-3")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldEqual mutable.Buffer(
      Effect("result", "List(Bar(b))"),
      Effect("update-child", "b-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldEqual mutable.Buffer()
  }

  it("splitOne stream") {

    val effects = mutable.Buffer[Effect[String]]()

    val bus = new EventBus[Id[Foo]]

    val owner = new TestableOwner

    val stream = bus.events.splitOne(_.id)((key, initialFoo, fooStream) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + initialFoo.version.toString)
      fooStream.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    stream.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldEqual mutable.Buffer()

    // --

    bus.writer.onNext(Foo("a", 1))

    effects shouldEqual mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("result", "Bar(a)"),
      Effect("update-child", "a-1")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 2))

    effects shouldEqual mutable.Buffer(
      Effect("result", "Bar(a)"), // this is a stream, not a signal, so it still emits this
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 1))

    effects shouldEqual mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("result", "Bar(b)"),
      Effect("update-child", "b-1")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2))

    effects shouldEqual mutable.Buffer(
      Effect("result", "Bar(b)"),
      Effect("update-child", "b-2")
    )

    effects.clear()

  }

}
