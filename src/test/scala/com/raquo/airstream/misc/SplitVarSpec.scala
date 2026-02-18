package com.raquo.airstream.misc

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{Observer, Signal, Transaction}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.{DynamicOwner, DynamicSubscription, ManualOwner, Subscription}
import com.raquo.airstream.split.{DuplicateKeysConfig, KeyedStrictSignal}
import com.raquo.airstream.split.KeyedStrictSignal.withKey
import com.raquo.airstream.state.Var
import com.raquo.ew.JsArray
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.mutable

// #Warning: this test is not in the `split` package to make sure that Scala 2.13 specific implicits
//  in the split package will be resolved correctly even outside of that package.

class SplitVarSpec extends UnitSpec with BeforeAndAfter {

  case class Foo(id: String, version: Int)

  case class Bar(id: String)

  case class Element(id: String, fooSignal: Signal[Foo]) {
    override def toString: String = s"Element($id, fooSignal)"
  }

  private val originalDuplicateKeysConfig = DuplicateKeysConfig.default

  after {
    DuplicateKeysConfig.setDefault(originalDuplicateKeysConfig)
  }

  def withOrWithoutDuplicateKeyWarnings(code: => Assertion): Assertion = {
    // This wrapper checks that behaviour is identical in both modes
    DuplicateKeysConfig.setDefault(DuplicateKeysConfig.noWarnings)
    withClue("DuplicateKeysConfig.shouldWarn=false")(code)
    DuplicateKeysConfig.setDefault(DuplicateKeysConfig.warnings)
    withClue("DuplicateKeysConfig.shouldWarn=true")(code)
  }

  it("split signal - raw semantics and lifecycle (1)") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

      val outerDynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

      val innerDynamicOwner = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

      // #Note: important to activate now, we're testing this (see comments below)
      outerDynamicOwner.activate()
      innerDynamicOwner.activate()

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.splitSeq(
        key = _.id, distinctOp = identity
      ) { fooVar =>
        val key = fooVar.key
        val initialFoo = fooVar.now()
        assert(key == initialFoo.id, "Key does not match initial value")
        effects += Effect(s"init-child-$key", key + "-" + initialFoo.version.toString)
        // @Note keep foreach / addObserver here – this is important.
        //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
        DynamicSubscription.subscribeCallback(
          innerDynamicOwner,
          owner =>
            fooVar.signal.foreach { foo =>
              assert(key == foo.id, "Subsequent value does not match initial key")
              effects += Effect(s"update-child-$key", foo.id + "-" + foo.version.toString)
            }(owner)
        )
        Bar(key)
      }

      DynamicSubscription.subscribeCallback(
        outerDynamicOwner,
        owner =>
          signal.foreach { result =>
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

      // effects.clear()
    }
  }

  it("split signal - raw semantics and lifecycle (2)") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

      val outerDynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

      val innerDynamicOwner = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

      // #Note: important to NOT activate the inner subscription right away, we're testing this (see comments below)
      outerDynamicOwner.activate()
      // innerDynamicOwner.activate()

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.signal.splitSeq(_.id, distinctOp = identity) { case KeyedStrictSignal(fooSignal, key) =>
        assert(key == fooSignal.now().id, "Key does not match initial value")
        effects += Effect(s"init-child-$key", key + "-" + fooSignal.now().version.toString)
        DynamicSubscription.subscribeCallback(
          innerDynamicOwner,
          owner =>
            fooSignal.foreach { foo =>
              assert(key == foo.id, "Subsequent value does not match initial key")
              effects += Effect(s"update-child-$key", foo.id + "-" + foo.version.toString)
            }(owner)
        )
        // #Note: Test that our dropping logic works does not break events scheduled after transaction boundary
        Transaction { _ =>
          DynamicSubscription.subscribeCallback(
            innerDynamicOwner,
            owner =>
              fooSignal.foreach { foo =>
                assert(key == foo.id, "Subsequent value does not match initial key [new-trx]")
                effects += Effect(s"new-trx-update-child-$key", foo.id + "-" + foo.version.toString)
              }(owner)
          )
        }
        Bar(key)
      }

      DynamicSubscription.subscribeCallback(
        outerDynamicOwner,
        owner =>
          signal.foreach { result =>
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

      // effects.clear()
    }
  }

  it("split signal - raw semantics and lifecycle (3)") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

      val outerDynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

      val innerDynamicOwner = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.signal.splitSeq(_.id, distinctOp = identity) { case fooSignal withKey key =>
        assert(key == fooSignal.now().id, "Key does not match initial value")
        effects += Effect(s"init-child-$key", key + "-" + fooSignal.now().version.toString)
        DynamicSubscription.subscribeCallback(
          innerDynamicOwner,
          owner =>
            fooSignal.foreach { foo =>
              assert(key == foo.id, "Subsequent value does not match initial key")
              effects += Effect(s"update-child-$key", foo.id + "-" + foo.version.toString)
            }(owner)
        )
        Bar(key)
      }

      DynamicSubscription.subscribeCallback(
        outerDynamicOwner,
        owner =>
          signal.foreach { result =>
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

      // effects.clear()
    }
  }

  it("split signal - raw semantics and lifecycle (4)") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

      val dynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.signal.splitSeq(_.id, distinctOp = identity) { case fooSignal withKey key =>
        assert(key == fooSignal.now().id, "Key does not match initial value")
        effects += Effect(s"init-child-$key", key + "-" + fooSignal.now().version.toString)
        Element(key, fooSignal)
      }

      DynamicSubscription.subscribeCallback(
        dynamicOwner,
        owner =>
          signal.foreach { result =>
            effects += Effect("result", result.toString)
            result.foreach { element =>
              DynamicSubscription.subscribeCallback(
                dynamicOwner,
                owner =>
                  element.fooSignal.foreach { foo =>
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
      // innerDynamicOwner.activate()

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

      // effects.clear()
    }
  }

  it("child split signal re-syncs with parent signal") {
    // https://github.com/raquo/Airstream/issues/120

    val owner = new ManualOwner

    val foosVar = Var[List[Foo]](Nil)

    var ownersById = Map[String, ManualOwner]()
    var fooSById = Map[String, Signal[Foo]]()
    var mapFooSById = Map[String, Signal[Option[Foo]]]()

    val splitSignal = foosVar.signal.splitSeq(_.id) { case fooS withKey id =>
      ownersById.get(id).foreach(_.killSubscriptions())

      val newOwner = new ManualOwner
      ownersById = ownersById.updated(id, newOwner)

      fooSById = fooSById.updated(id, fooS)

      val mapFooS = foosVar.signal.map(_.find(_.id == id))
      mapFooSById = mapFooSById.updated(id, mapFooS)
    }

    // --

    val splitSub = splitSignal.addObserver(Observer.empty)(owner)

    assert(ownersById.isEmpty)
    assert(fooSById.isEmpty)
    assert(mapFooSById.isEmpty)

    // --

    foosVar.set(Foo("a", 1) :: Nil)

    val owner_A = ownersById("a")
    val fooS_A = fooSById("a")
    val mapFooS_A = mapFooSById("a")

    val fooS_A_observed_1 = fooS_A.observe(owner)
    val mapFooS_A_observed_1 = mapFooS_A.observe(owner)

    foosVar.set(Foo("a", 2) :: Foo("b", 1) :: Nil)

    assert(ownersById("a") eq owner_A)
    assert(fooSById("a") eq fooS_A)
    assert(mapFooSById("a") eq mapFooS_A)

    assert(fooS_A_observed_1.now() == Foo("a", 2))
    assert(mapFooS_A_observed_1.now().contains(Foo("a", 2)))

    // --

    fooS_A_observed_1.killOriginalSubscription()
    mapFooS_A_observed_1.killOriginalSubscription()

    foosVar.set(Foo("b", 2) :: Foo("a", 3) :: Nil)

    val owner_B = ownersById("b")
    val fooS_B = fooSById("b")
    val mapFooS_B = mapFooSById("b")

    val fooS_B_observed_1 = fooS_B.observe(owner_B)
    val mapFooS_B_observed_1 = mapFooS_B.observe(owner_B)

    assert(ownersById("b") eq owner_B)
    assert(fooSById("b") eq fooS_B)
    assert(mapFooSById("b") eq mapFooS_B)

    // Verifying that these signals don't update after their subs getting killed
    assert(fooS_A_observed_1.now() == Foo("a", 2))
    assert(mapFooS_A_observed_1.now().contains(Foo("a", 2)))

    // Verifying that if the start of the child signal is delayed,
    // the child signal still picks up the most recent value,
    // and not the initial value that it was instantiated with.
    assert(fooS_B_observed_1.now() == Foo("b", 2))
    assert(mapFooS_B_observed_1.now().contains(Foo("b", 2)))

    // --

    foosVar.set(Foo("a", 4) :: Foo("b", 3) :: Nil)

    assert(ownersById("a") eq owner_A)
    assert(fooSById("a") eq fooS_A)
    assert(mapFooSById("a") eq mapFooS_A)

    assert(fooS_B_observed_1.now() == Foo("b", 3))
    assert(mapFooS_B_observed_1.now().contains(Foo("b", 3)))

    // --

    val fooS_A_observed_2 = fooS_A.observe(owner)
    val mapFooS_A_observed_2 = mapFooS_A.observe(owner)

    assert(fooS_A_observed_2.now() == Foo("a", 4))
    assert(mapFooS_A_observed_2.now().contains(Foo("a", 4)))

    // --

    foosVar.set(Foo("a", 5) :: Nil)

    assert(ownersById("a") eq owner_A)
    assert(fooSById("a") eq fooS_A)
    assert(mapFooSById("a") eq mapFooS_A)

    assert(fooS_A_observed_2.now() == Foo("a", 5))
    assert(mapFooS_A_observed_2.now().contains(Foo("a", 5)))
  }

  it("child split signal re-syncs with parent stream") {
    // https://github.com/raquo/Airstream/issues/120

    val owner = new ManualOwner

    val foosBus = new EventBus[List[Foo]]

    var ownersById = Map[String, ManualOwner]()
    var fooSById = Map[String, Signal[Foo]]()
    var mapFooSById = Map[String, Signal[Option[Foo]]]()

    val splitSignal = foosBus.stream.splitSeq(_.id) { case fooS withKey id =>
      ownersById.get(id).foreach(_.killSubscriptions())

      val newOwner = new ManualOwner
      ownersById = ownersById.updated(id, newOwner)

      fooSById = fooSById.updated(id, fooS)

      val mapFooS = foosBus.stream.startWith(Nil).map(_.find(_.id == id))
      mapFooSById = mapFooSById.updated(id, mapFooS)
    }

    // --

    val splitSub = splitSignal.addObserver(Observer.empty)(owner)

    assert(ownersById.isEmpty)
    assert(fooSById.isEmpty)
    assert(mapFooSById.isEmpty)

    // --

    foosBus.emit(Foo("a", 1) :: Nil)

    val owner_A = ownersById("a")
    val fooS_A = fooSById("a")
    val mapFooS_A = mapFooSById("a")

    val fooS_A_observed_1 = fooS_A.observe(owner_A)
    val mapFooS_A_observed_1 = mapFooS_A.observe(owner_A)

    foosBus.emit(Foo("a", 2) :: Foo("b", 1) :: Nil)

    assert(ownersById("a") eq owner_A)
    assert(fooSById("a") eq fooS_A)
    assert(mapFooSById("a") eq mapFooS_A)

    assert(fooS_A_observed_1.now() == Foo("a", 2))
    assert(mapFooS_A_observed_1.now().contains(Foo("a", 2)))

    // --

    fooS_A_observed_1.killOriginalSubscription()
    mapFooS_A_observed_1.killOriginalSubscription()

    foosBus.emit(Foo("b", 2) :: Foo("a", 3) :: Nil)

    val owner_B = ownersById("b")
    val fooS_B = fooSById("b")
    val mapFooS_B = mapFooSById("b")

    val fooS_B_observed_1 = fooS_B.observe(owner_B)
    val mapFooS_B_observed_1 = mapFooS_B.observe(owner_B)

    assert(ownersById("b") eq owner_B)
    assert(fooSById("b") eq fooS_B)
    assert(mapFooSById("b") eq mapFooS_B)

    // Verifying that these signals don't update after their subs getting killed
    assert(fooS_A_observed_1.now() == Foo("a", 2))
    assert(mapFooS_A_observed_1.now().contains(Foo("a", 2)))

    // Verifying that if the start of the child signal is delayed,
    // the child signal still picks up the most recent value,
    // and not the initial value that it was instantiated with.
    assert(fooS_B_observed_1.now() == Foo("b", 2))
    assert(mapFooS_B_observed_1.now().isEmpty) // this is based on stream so it can't actually re-sync

    // --

    foosBus.emit(Foo("a", 4) :: Foo("b", 3) :: Nil)

    assert(ownersById("a") eq owner_A)
    assert(fooSById("a") eq fooS_A)
    assert(mapFooSById("a") eq mapFooS_A)

    assert(fooS_B_observed_1.now() == Foo("b", 3))
    assert(mapFooS_B_observed_1.now().contains(Foo("b", 3)))

    // --

    val fooS_A_observed_2 = fooS_A.observe(owner)
    val mapFooS_A_observed_2 = mapFooS_A.observe(owner)

    assert(fooS_A_observed_2.now() == Foo("a", 4))
    assert(mapFooS_A_observed_2.now().contains(Foo("a", 2))) // this is based on stream so it can't actually re-sync

    // --

    foosBus.emit(Foo("a", 5) :: Nil)

    assert(ownersById("a") eq owner_A)
    assert(fooSById("a") eq fooS_A)
    assert(mapFooSById("a") eq mapFooS_A)

    assert(fooS_A_observed_2.now() == Foo("a", 5))
    assert(mapFooS_A_observed_2.now().contains(Foo("a", 5)))
  }

  it("split mutable array") {
    val effects = mutable.Buffer[Effect[String]]()

    // #TODO[Test] Would be nice to also verify this with immutable.Seq
    //  as their implementations are separate.

    val arr = JsArray(Foo("initial", 1))

    val myVar = Var(arr)

    val owner = new TestableOwner

    val signal = myVar.signal.splitSeq(_.id) { case fooSignal withKey key =>
      assert(key == fooSignal.now().id, "Key does not match initial value")
      effects += Effect("init-child", key + "-" + fooSignal.now().version.toString)
      fooSignal.foreach { foo =>
        assert(key == foo.id, "Subsequent value does not match initial key")
        effects += Effect("update-child", foo.id + "-" + foo.version.toString)
      }(owner)
      Bar(key)
    }

    signal.foreach { result =>
      effects += Effect("result", result.toString)
    }(owner)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "initial-1"),
      Effect("update-child", "initial-1"),
      Effect("result", "Bar(initial)") // Single item JS Array is printed this way
    )

    effects.clear()

    // --

    arr.update(0, Foo("a", 1))

    myVar.set(arr)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "a-1"),
      Effect("update-child", "a-1"),
      Effect("result", "Bar(a)") // Single item JS Array is printed this way
    )

    effects.clear()

    // --

    arr.update(0, Foo("a", 2))

    myVar.set(arr)

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(a)"), // Single item JS Array is printed this way
      Effect("update-child", "a-2")
    )

    effects.clear()

    // --

    arr.update(0, Foo("a", 3))
    arr.push(Foo("b", 1))

    myVar.set(arr)

    effects shouldBe mutable.Buffer(
      Effect("init-child", "b-1"),
      Effect("update-child", "b-1"),
      Effect("result", "Bar(a),Bar(b)"), // Multi item JS Array is printed this way
      Effect("update-child", "a-3")
    )

    effects.clear()

    // --

    arr.reverse()

    myVar.set(arr)

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(b),Bar(a)") // Multi item JS Array is printed this way
    )

    effects.clear()

    // --

    arr.update(0, Foo("b", 2))
    arr.pop()

    myVar.set(arr)

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(b)"), // Single item JS Array is printed this way
      Effect("update-child", "b-2")
    )

    effects.clear()

    // --

    myVar.writer.onNext(arr)

    effects shouldBe mutable.Buffer(
      Effect("result", "Bar(b)") // Single item JS Array is printed this way
    )

    // effects.clear()
  }

  it("splitByIndex var - quick check") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](Foo("initial", 1) :: Nil)

      val owner = new TestableOwner

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.splitSeqByIndex { fooVar =>
        val index = fooVar.key
        val initialFoo = fooVar.now()
        effects += Effect(s"init-child-$index", initialFoo.id + "-" + initialFoo.version.toString)
        fooVar.signal.foreach { foo =>
          effects += Effect(s"update-child-$index", foo.id + "-" + foo.version.toString)
        }(owner)
        Bar(index.toString)
      }

      signal.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)

      effects shouldBe mutable.Buffer(
        Effect("init-child-0", "initial-1"),
        Effect("update-child-0", "initial-1"),
        Effect("result", "List(Bar(0))")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("a", 1) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0))"),
        Effect("update-child-0", "a-1"),
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("a", 2) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0))"),
        Effect("update-child-0", "a-2")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("a", 3) :: Foo("b", 1) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child-1", "b-1"),
        Effect("update-child-1", "b-1"),
        Effect("result", "List(Bar(0), Bar(1))"),
        Effect("update-child-0", "a-3")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("b", 1) :: Foo("a", 3) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0), Bar(1))"),
        Effect("update-child-0", "b-1"),
        Effect("update-child-1", "a-3")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("b", 1) :: Foo("a", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0), Bar(1))"),
        Effect("update-child-1", "a-4")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("b", 2) :: Foo("a", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0), Bar(1))"),
        Effect("update-child-0", "b-2")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("b", 3) :: Foo("a", 5) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0), Bar(1))"),
        Effect("update-child-0", "b-3"),
        Effect("update-child-1", "a-5")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("b", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0))"),
        Effect("update-child-0", "b-4")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Foo("b", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(0))")
      )

      // effects.clear()
    }
  }

  it("splitOption var") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[Option[Foo]](Some(Foo("initial", 1)))

      val owner = new TestableOwner

      var maybeLastSub: Option[Subscription] = None

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.splitOption(
        fooVar => {
          val initialFoo = fooVar.now()
          val initialKey = s"${initialFoo.id}-${initialFoo.version}"
          effects += Effect(s"init-child-$initialKey", initialKey)
          // #Note: this manual management isn't great, but we don't have Laminar's mounting system here
          maybeLastSub.foreach(_.kill())
          maybeLastSub = Some(fooVar.signal.foreach { foo =>
            val updatedKey = s"${foo.id}-${foo.version}"
            effects += Effect(s"update-child-$updatedKey", updatedKey)
          }(owner))
          Bar(initialKey)
        },
        ifEmpty = {
          effects += Effect("ifEmpty-eval", "")
          Bar("empty")
        }
      )

      // --

      signal.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)

      effects shouldBe mutable.Buffer(
        Effect("init-child-initial-1", "initial-1"),
        Effect("update-child-initial-1", "initial-1"),
        Effect("result", "Bar(initial-1)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Some(Foo("a", 1)))

      effects shouldBe mutable.Buffer(
        Effect("result", "Bar(initial-1)"), // we use initialKey when returning Bar, so it's `initial-1`, not `a-1`
        Effect("update-child-a-1", "a-1")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Some(Foo("a", 2)))

      effects shouldBe mutable.Buffer(
        Effect("result", "Bar(initial-1)"), // we use initialKey when returning Bar, so it's `initial-1`, not `a-2`
        Effect("update-child-a-2", "a-2")
      )

      effects.clear()

      // --

      myVar.writer.onNext(None)

      effects shouldBe mutable.Buffer(
        Effect("ifEmpty-eval", ""),
        Effect("result", "Bar(empty)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(None)

      effects shouldBe mutable.Buffer()

      // --

      myVar.writer.onNext(Some(Foo("c", 1)))

      effects shouldBe mutable.Buffer(
        Effect("init-child-c-1", "c-1"),
        Effect("update-child-c-1", "c-1"),
        Effect("result", "Bar(c-1)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(Some(Foo("c", 2)))

      effects shouldBe mutable.Buffer(
        Effect("result", "Bar(c-1)"), // we use initialKey when returning Bar, so it's `c-1`, not `c-2`
        Effect("update-child-c-2", "c-2")
      )

      // effects.clear()
    }
  }

  it("unused child var behaviour") {

    // If user does not actually subscribe to `fooVar`, we want to make
    // sure that pulling its value with .now() still produces correct
    // current value.

    val owner = new TestableOwner

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val fooVars: mutable.Map[String, Var[Foo]] = mutable.Map()

    val foosVar = Var[List[Foo]](Nil).setDisplayName("foosVar")

    foosVar.splitSeq(_.id) { fooVar =>
      val id = fooVar.key
      fooVar.setDisplayName(s"fooVar-${id}")
      effects += s"create-${fooVar.now().toString}"
      fooVars.update(id, fooVar)
      Element(id, fooVar.signal)
    }.foreach(_ => ())(owner)

    assertEquals(effects.toList, Nil)

    // --

    foosVar.set(Foo("1", 10) :: Foo("2", 10) :: Foo("3", 10) :: Nil)

    assertEquals(
      effects.toList,
      List(
        """create-Foo(1,10)""",
        """create-Foo(2,10)""",
        """create-Foo(3,10)""",
      )
    )
    effects.clear()

    // --

    foosVar.update(list => list.updated(0, list.head.copy(version = 20)))

    assertEquals(effects.toList, Nil)

    assertEquals(fooVars("1").now(), Foo("1", 20))

    // --

    fooVars("1").set(Foo("1", 30))

    assertEquals(effects.toList, Nil)

    assertEquals(fooVars("1").now(), Foo("1", 30))
    assertEquals(fooVars("2").now(), Foo("2", 10))
    assertEquals(fooVars("3").now(), Foo("3", 10))
  }
}
