package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{Signal, Transaction}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.{DynamicOwner, DynamicSubscription}
import com.raquo.airstream.state.Var

import scala.collection.{immutable, mutable}
import scala.scalajs.js

// #Warning: this test is not in the `split` package to make sure that Scala 2.12 and 2.13 specific implicits
//  in the split package will be resolved correctly even outside of that package.

class SplitSignalSpec extends UnitSpec {

  case class Foo(id: String, version: Int)

  case class Bar(id: String)

  case class Element(id: String, fooSignal: Signal[Foo]) {
    override def toString: String = s"Element($id, fooSignal)"
  }

  it("splits stream into signals") {

    val effects = mutable.Buffer[Effect[String]]()

    val bus = new EventBus[List[Foo]]

    val owner = new TestableOwner

    val stream = bus.events.split(_.id)(project = (key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + initialFoo.version.toString)
      // @Note keep foreach or addObserver here – this is important.
      //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
      fooSignal.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    stream.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("result", "List()")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 1) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("update-child", "a-1"),
      Effect("result", "List(Bar(a))")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 2) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(a))"),
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 3) :: Foo("b", 1) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("update-child", "b-1"),
      Effect("result", "List(Bar(a), Bar(b))"),
      Effect("update-child", "a-3")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b))"),
      Effect("update-child", "b-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b))") // output is a stream, not signal
    )

  }

  it("split signal into signals") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

    val owner = new TestableOwner

    val signal = myVar.signal.split(_.id)(project = (key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + initialFoo.version.toString)
      // @Note keep foreach or addObserver here – this is important.
      //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
      fooSignal.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    signal.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "initial-1"),
      Effect("update-child", "initial-1"),
      Effect("result", "List(Bar(initial))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 1) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("update-child", "a-1"),
      Effect("result", "List(Bar(a))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 2) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(a))"),
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 3) :: Foo("b", 1) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("update-child", "b-1"),
      Effect("result", "List(Bar(a), Bar(b))"),
      Effect("update-child", "a-3")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b))"),
      Effect("update-child", "b-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 2) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b))")
    )

    effects.clear()
  }

  it("splitOne stream") {

    val effects = mutable.Buffer[Effect[String]]()

    val bus = new EventBus[Foo]

    val owner = new TestableOwner

    val stream = bus.events.splitOne(_.id)((key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + initialFoo.version.toString)
      fooSignal.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    stream.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer()

    // --

    bus.writer.onNext(Foo("a", 1))

    effects shouldBe mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("update-child", "a-1"),
      Effect("result", "Bar(a)")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("a", 2))

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(a)"),
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 1))

    effects shouldBe mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("update-child", "b-1"),
      Effect("result", "Bar(b)")
    )

    effects.clear()

    // --

    bus.writer.onNext(Foo("b", 2))

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(b)"),
      Effect("update-child", "b-2")
    )

    effects.clear()

  }

  it("splitOne signal") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var(Foo("initial", 1))

    val owner = new TestableOwner

    val splitSignal = myVar.signal.splitOne(_.id)((key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect(s"init-child-$key", key + "-" + initialFoo.version.toString)
      fooSignal.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect(s"update-child-${key}", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    splitSignal.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("init-child-initial", "initial-1"),
      Effect("update-child-initial", "initial-1"),
      Effect("result", "Bar(initial)")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 1))

    effects shouldBe mutable.Buffer(
      Effect("init-child-a", "a-1"),
      Effect("update-child-a", "a-1"),
      Effect("result", "Bar(a)"),
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 2))

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(a)"), // this is a stream, not a signal, so it still emits this
      Effect("update-child-a", "a-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1))

    effects shouldBe mutable.Buffer(
      Effect("init-child-b", "b-1"),
      Effect("update-child-b", "b-1"),
      Effect("result", "Bar(b)")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 2))

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(b)"),
      Effect("update-child-b", "b-2")
    )

    effects.clear()

  }

  it("split signal - raw semantics") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

    val owner = new TestableOwner

    // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
    //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
    val signal = myVar.signal.split(_.id, distinctCompose = identity)(project = (key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect(s"init-child-$key", key + "-" + initialFoo.version.toString)
      // @Note keep foreach / addObserver here – this is important.
      //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
      fooSignal.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect(s"update-child-$key", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    })

    signal.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("init-child-initial", "initial-1"),
      Effect("update-child-initial", "initial-1"),
      Effect("result", "List(Bar(initial))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 1) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child-a", "a-1"),
      Effect("update-child-a", "a-1"),
      Effect("result", "List(Bar(a))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 2) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(a))"),
      Effect("update-child-a", "a-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 3) :: Foo("b", 1) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child-b", "b-1"),
      Effect("update-child-b", "b-1"),
      Effect("result", "List(Bar(a), Bar(b))"),
      Effect("update-child-a", "a-3")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))"),
      Effect("update-child-a", "a-3"),
      Effect("update-child-b", "b-1")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 4) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))"),
      Effect("update-child-a", "a-4"),
      Effect("update-child-b", "b-1")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 2) :: Foo("a", 4) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))"),
      Effect("update-child-a", "a-4"),
      Effect("update-child-b", "b-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 3) :: Foo("a", 5) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))"),
      Effect("update-child-a", "a-5"),
      Effect("update-child-b", "b-3")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 4) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b))"),
      Effect("update-child-b", "b-4")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 4) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b))"),
      Effect("update-child-b", "b-4")
    )

    effects.clear()
  }

  it("split signal - raw semantics and lifecycle (1)") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

    val outerDynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

    val innerDynamicOwner = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

    // #Note: important to activate now, we're testing this (see comments below)
    outerDynamicOwner.activate()
    innerDynamicOwner.activate()

    // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
    //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
    val signal = myVar.signal.split(_.id, distinctCompose = identity)(project = (key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect(s"init-child-$key", key + "-" + initialFoo.version.toString)
      // @Note keep foreach / addObserver here – this is important.
      //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
      DynamicSubscription.subscribeCallback(
        innerDynamicOwner,
        owner => fooSignal.foreach { foo =>
          assert(key == foo.id, "Subsequent value does not match initial key")
          effects += Effect(s"update-child-$key", foo.id + "-" + foo.version.toString)
        }(owner)
      )
      Bar(key)
    })

    DynamicSubscription.subscribeCallback(
      outerDynamicOwner,
      owner => signal.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)
    )

    effects shouldBe mutable.Buffer(
      Effect("init-child-initial", "initial-1"),
      Effect("update-child-initial", "initial-1"),
      Effect("result", "List(Bar(initial))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child-b", "b-1"),
      Effect("update-child-b", "b-1"),
      Effect("init-child-a", "a-3"),
      Effect("update-child-a", "a-3"),
      Effect("result", "List(Bar(b), Bar(a))")
    )

    effects.clear()

    // --

    outerDynamicOwner.deactivate()
    innerDynamicOwner.deactivate()

    effects shouldBe mutable.Buffer()

    // --

    outerDynamicOwner.activate()
    innerDynamicOwner.activate()

    effects shouldBe mutable.Buffer(
      // #Note `initial` is here because our code created an inner subscription for `initial`
      //  and kept it alive even after the element was removed. This inner signal itself will
      //  not receive any updates until "initial" key is added to the inputs again (actually
      //  it might cause issues in this pattern if this happens), but this inner signal's
      //  current value is still sent to the observer when we re-activate its dynamic owner.
      Effect("result", "List(Bar(b), Bar(a))"),
      Effect("update-child-initial", "initial-1"),
      Effect("update-child-b", "b-1"),
      Effect("update-child-a", "a-3"),
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 2) :: Foo("a", 3) :: Nil)

    // This assertion makes sure that `resetOnStop` is set correctly in `drop(1, resetOnStop = false)`
    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))"),
      Effect("update-child-b", "b-2"),
      Effect("update-child-a", "a-3"),
    )

    effects.clear()

    // --

    outerDynamicOwner.deactivate()

    effects shouldBe mutable.Buffer()

    // --

    outerDynamicOwner.activate()

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(b), Bar(a))")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("a", 4) :: Foo("b", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("result", "List(Bar(a), Bar(b))"),
      Effect("update-child-b", "b-3"),
      Effect("update-child-a", "a-4"),
    )

    effects.clear()
  }

  it("split signal - raw semantics and lifecycle (2)") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

    val outerDynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

    val innerDynamicOwner = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

    // #Note: important to NOT activate the inner subscription right away, we're testing this (see comments below)
    outerDynamicOwner.activate()
    //innerDynamicOwner.activate()

    // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
    //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
    val signal = myVar.signal.split(_.id, distinctCompose = identity)(project = (key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect(s"init-child-$key", key + "-" + initialFoo.version.toString)
      DynamicSubscription.subscribeCallback(
        innerDynamicOwner,
        owner => fooSignal.foreach { foo =>
          assert(key == foo.id, "Subsequent value does not match initial key")
          effects += Effect(s"update-child-$key", foo.id + "-" + foo.version.toString)
        }(owner)
      )
      // #Note: Test that our dropping logic works does not break events scheduled after transaction boundary
      new Transaction(_ => {
        DynamicSubscription.subscribeCallback(
          innerDynamicOwner,
          owner => fooSignal.foreach { foo =>
            assert(key == foo.id, "Subsequent value does not match initial key [new-trx]")
            effects += Effect(s"new-trx-update-child-$key", foo.id + "-" + foo.version.toString)
          }(owner)
        )
      })
      Bar(key)
    })

    DynamicSubscription.subscribeCallback(
      outerDynamicOwner,
      owner => signal.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)
    )

    effects shouldBe mutable.Buffer(
      Effect("init-child-initial", "initial-1"),
      Effect("result", "List(Bar(initial))")
    )

    effects.clear()

    // --

    innerDynamicOwner.activate()

    effects shouldBe mutable.Buffer(
      Effect("update-child-initial", "initial-1"),
      Effect("new-trx-update-child-initial", "initial-1"),
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child-b", "b-1"),
      Effect("update-child-b", "b-1"),
      Effect("init-child-a", "a-3"),
      Effect("update-child-a", "a-3"),
      Effect("result", "List(Bar(b), Bar(a))"),
      Effect("new-trx-update-child-b", "b-1"),
      Effect("new-trx-update-child-a", "a-3")
    )

    effects.clear()
  }

  it("split signal - raw semantics and lifecycle (3)") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

    val outerDynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

    val innerDynamicOwner = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

    // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
    //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
    val signal = myVar.signal.split(_.id, distinctCompose = identity)(project = (key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect(s"init-child-$key", key + "-" + initialFoo.version.toString)
      DynamicSubscription.subscribeCallback(
        innerDynamicOwner,
        owner => fooSignal.foreach { foo =>
          assert(key == foo.id, "Subsequent value does not match initial key")
          effects += Effect(s"update-child-$key", foo.id + "-" + foo.version.toString)
        }(owner)
      )
      Bar(key)
    })

    DynamicSubscription.subscribeCallback(
      outerDynamicOwner,
      owner => signal.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)
    )

    effects shouldBe mutable.Buffer()

    // --

    outerDynamicOwner.activate()
    innerDynamicOwner.activate()

    effects shouldBe mutable.Buffer(
      Effect("init-child-initial", "initial-1"),
      Effect("result", "List(Bar(initial))"),
      Effect("update-child-initial", "initial-1")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child-b", "b-1"),
      Effect("update-child-b", "b-1"),
      Effect("init-child-a", "a-3"),
      Effect("update-child-a", "a-3"),
      Effect("result", "List(Bar(b), Bar(a))")
    )

    effects.clear()
  }

  it("split signal - raw semantics and lifecycle (4)") {

    val effects = mutable.Buffer[Effect[String]]()

    val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

    val dynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

    // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
    //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
    val signal = myVar.signal.split(_.id, distinctCompose = identity)(project = (key, initialFoo, fooSignal) => {
      assert(key == initialFoo.id, "Key does not match initial value")
      effects += Effect(s"init-child-$key", key + "-" + initialFoo.version.toString)
      Element(key, fooSignal)
    })

    DynamicSubscription.subscribeCallback(
      dynamicOwner,
      owner => signal.foreach { result =>
        effects += Effect("result", result.toString)
        result.foreach { element =>
          DynamicSubscription.subscribeCallback(
            dynamicOwner,
            owner => element.fooSignal.foreach { foo =>
              assert(element.id == foo.id, "Subsequent value does not match initial key")
              effects += Effect(s"update-child-${element.id}", foo.id + "-" + foo.version.toString)
            }(owner)
          )
        }
      }(owner)
    )

    effects shouldBe mutable.Buffer()

    // --

    dynamicOwner.activate()
    //innerDynamicOwner.activate()

    effects shouldBe mutable.Buffer(
      Effect("init-child-initial", "initial-1"),
      Effect("result", "List(Element(initial, fooSignal))"),
      Effect("update-child-initial", "initial-1")
    )

    effects.clear()

    // --

    myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

    effects shouldBe mutable.Buffer(
      Effect("init-child-b", "b-1"),
      Effect("init-child-a", "a-3"),
      Effect("result", "List(Element(b, fooSignal), Element(a, fooSignal))"),
      Effect("update-child-b", "b-1"),
      Effect("update-child-a", "a-3")
    )

    effects.clear()
  }

  it("split list / vector / set / js.array / immutable.seq / collection.seq / option compiles") {
    // Having this test pass on all supported Scala versions is important to ensure that the implicits are actually usable.
    {
      (new EventBus[List[Foo]]).events.split(_.id)((_, _, _) => 100)
      (new EventBus[Vector[Foo]]).events.split(_.id)((_, _, _) => 100)
      (new EventBus[Set[Foo]]).events.split(_.id)((_, _, _) => 100)
      (new EventBus[js.Array[Foo]]).events.split(_.id)((_, _, _) => 100)
      (new EventBus[immutable.Seq[Foo]]).events.split(_.id)((_, _, _) => 100)
      (new EventBus[collection.Seq[Foo]]).events.split(_.id)((_, _, _) => 100)
      (new EventBus[collection.Seq[Foo]]).events.split(_.id)((_, _, _) => 100)
    }
    // And now the same, but with `distinctCompose = identity`, because that somehow affects implicit resolution in Scala 3.0.0
    {
      (new EventBus[List[Foo]]).events.split(_.id, identity)((_, _, _) => 100)
      (new EventBus[Vector[Foo]]).events.split(_.id, identity)((_, _, _) => 100)
      (new EventBus[Set[Foo]]).events.split(_.id, identity)((_, _, _) => 100)
      (new EventBus[js.Array[Foo]]).events.split(_.id, identity)((_, _, _) => 100)
      (new EventBus[immutable.Seq[Foo]]).events.split(_.id, identity)((_, _, _) => 100)
      (new EventBus[collection.Seq[Foo]]).events.split(_.id, identity)((_, _, _) => 100)
      (new EventBus[collection.Seq[Foo]]).events.split(_.id, identity)((_, _, _) => 100)
    }
  }

  it("split option by isDefined compiles") {

    val bus = new EventBus[Option[Foo]]

    val _ = bus.events.split(_ => ())((_, _, _) => 100).map(_.getOrElse(0))
  }
}
