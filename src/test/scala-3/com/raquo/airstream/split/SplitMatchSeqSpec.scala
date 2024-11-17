package com.raquo.airstream.split

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{Observer, Signal, Transaction}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.{DynamicOwner, DynamicSubscription, ManualOwner, Subscription}
import com.raquo.airstream.split.DuplicateKeysConfig
import com.raquo.airstream.split.SplitMatchSeqMacros.*
import com.raquo.airstream.state.Var
import com.raquo.ew.{JsArray, JsVector}
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.collection.{immutable, mutable}
import scala.scalajs.js
import com.raquo.airstream.state.Var.update

class SplitMatchSeqSpec extends UnitSpec with BeforeAndAfter {

  sealed trait Foo {
    def id: String
  }

  case class FooC(id: String, version: Int) extends Foo

  object FooO extends Foo {
    override val id: String = "object"
  }

  enum FooE(val numOpt: Option[Int]) extends Foo {
    override def id: String = numOpt.map(num => s"int_$num").getOrElse("int_-1")

    case FooE1 extends FooE(Some(0))
    case FooE2 extends FooE(Some(1))
    case FooE3 extends FooE(None)
  }

  object FooE {
    def unapply(fooE: FooE): Some[Option[Int]] = Some(fooE.numOpt)
  }

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

  it("splits stream into signals") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val bus = new EventBus[List[Foo]]

      val owner = new TestableOwner

      val stream = bus.stream
        .splitMatchSeq(_.id)
        .handleCase {
          case FooE(Some(num)) => num
          case FooE(None) => -1
        } { (initialNum, numSignal) =>
          effects += Effect("init-child", s"FooE($initialNum)")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          numSignal.foreach { num =>
            assert(initialNum == num, "Subsequent value does not match initial key")
            effects += Effect("update-child", s"FooE($num)")
          }(owner)
          Bar(s"$initialNum")
        }
        .handleType[FooC] { (initialFooC, fooCSignal) =>
          effects += Effect("init-child", s"FooC(${initialFooC.id}-${initialFooC.version})")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          fooCSignal.foreach { fooC =>
            assert(initialFooC.id == fooC.id, "Subsequent value does not match initial key")
            effects += Effect("update-child", s"FooC(${fooC.id}-${fooC.version})")
          }(owner)
          Bar(initialFooC.id)
        }
        .handleValue(FooO) {
          effects += Effect("init-child", s"FooO(${FooO.id})")
          Bar(FooO.id)
        }
        .toSignal

      stream.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)

      effects shouldBe mutable.Buffer(
        Effect("result", "List()")
      )

      effects.clear()

      // --

      bus.writer.onNext(FooE.FooE1 :: FooC("a", 1) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child", "FooE(0)"),
        Effect("update-child", "FooE(0)"),
        Effect("init-child", "FooC(a-1)"),
        Effect("update-child", "FooC(a-1)"),
        Effect("result", "List(Bar(0), Bar(a))")
      )

      effects.clear()

      // --

      bus.writer.onNext(FooC("a", 2) :: FooE.FooE3 :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child", "FooE(-1)"),
        Effect("update-child", "FooE(-1)"),
        Effect("result", "List(Bar(a), Bar(-1))"),
        Effect("update-child", "FooC(a-2)")
      )

      effects.clear()

      // --

      bus.writer.onNext(FooE.FooE2 :: FooC("a", 3) :: FooC("b", 1) :: FooO :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child", "FooE(1)"),
        Effect("update-child", "FooE(1)"),
        Effect("init-child", "FooC(b-1)"),
        Effect("update-child", "FooC(b-1)"),
        Effect("init-child", "FooO(object)"),
        Effect("result", "List(Bar(1), Bar(a), Bar(b), Bar(object))"),
        Effect("update-child", "FooC(a-3)")
      )

      effects.clear()

      // --

      bus.writer.onNext(FooC("b", 1) :: FooO :: FooC("a", 3) :: FooE.FooE2 :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(object), Bar(a), Bar(1))")
      )

      effects.clear()

      // --

      bus.writer.onNext(FooE.FooE2 :: FooC("b", 2) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(1), Bar(b))"),
        Effect("update-child", "FooC(b-2)")
      )

      effects.clear()

      // --

      bus.writer.onNext(FooC("b", 2) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b))") // output is a stream, not signal
      )
    }
  }

  it("split signal into signals") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](FooC("initial", 1) :: FooE.FooE1 :: FooO :: Nil)

      val owner = new TestableOwner

      val signal = myVar.signal.splitMatchSeq(_.id)
        .handleCase {
          case FooE(Some(num)) => num
          case FooE(None) => -1
        } { (initialNum, numSignal) =>
          effects += Effect("init-child", s"FooE($initialNum)")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          numSignal.foreach { num =>
            assert(initialNum == num, "Subsequent value does not match initial key")
            effects += Effect("update-child", s"FooE($num)")
          }(owner)
          Bar(s"$initialNum")
        }
        .handleType[FooC] { (initialFooC, fooCSignal) =>
          effects += Effect("init-child", s"FooC(${initialFooC.id}-${initialFooC.version})")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          fooCSignal.foreach { fooC =>
            assert(initialFooC.id == fooC.id, "Subsequent value does not match initial key")
            effects += Effect("update-child", s"FooC(${fooC.id}-${fooC.version})")
          }(owner)
          Bar(initialFooC.id)
        }
        .handleValue(FooO) {
          effects += Effect("init-child", s"FooO(${FooO.id})")
          Bar(FooO.id)
        }
        .toSignal

      signal.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)

      effects shouldBe mutable.Buffer(
        Effect("init-child", "FooC(initial-1)"),
        Effect("update-child", "FooC(initial-1)"),
        Effect("init-child", "FooE(0)"),
        Effect("update-child", "FooE(0)"),
        Effect("init-child", "FooO(object)"),
        Effect("result", "List(Bar(initial), Bar(0), Bar(object))")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooO :: FooC("a", 1) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child", "FooC(a-1)"),
        Effect("update-child", "FooC(a-1)"),
        Effect("result", "List(Bar(object), Bar(a))")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("a", 2) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(a))"),
        Effect("update-child", "FooC(a-2)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("a", 3) :: FooE.FooE2 :: FooC("b", 1) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child", "FooE(1)"),
        Effect("update-child", "FooE(1)"),
        Effect("init-child", "FooC(b-1)"),
        Effect("update-child", "FooC(b-1)"),
        Effect("result", "List(Bar(a), Bar(1), Bar(b))"),
        Effect("update-child", "FooC(a-3)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("b", 1) :: FooC("a", 3) :: FooE.FooE2 :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(a), Bar(1))")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooE.FooE3 :: FooC("b", 2) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child", "FooE(-1)"),
        Effect("update-child", "FooE(-1)"),
        Effect("result", "List(Bar(-1), Bar(b))"),
        Effect("update-child", "FooC(b-2)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("b", 2) :: FooE.FooE3 :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(-1))")
      )

      //effects.clear()
    }
  }

  it("split signal - raw semantics") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](FooC("initial", 1) :: FooE.FooE1 :: FooO :: Nil)

      val owner = new TestableOwner

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.signal.splitMatchSeq(_.id, distinctCompose = identity)
        .handleCase {
          case FooE(Some(num)) => num
          case FooE(None) => -1
        } { (initialNum, numSignal) =>
          effects += Effect(s"init-child-$initialNum", s"FooE($initialNum)")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          numSignal.foreach { num =>
            assert(initialNum == num, "Subsequent value does not match initial key")
            effects += Effect(s"update-child-$num", s"FooE($num)")
          }(owner)
          Bar(s"$initialNum")
        }
        .handleType[FooC] { (initialFooC, fooCSignal) =>
          effects += Effect(s"init-child-${initialFooC.id}", s"FooC(${initialFooC.id}-${initialFooC.version})")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          fooCSignal.foreach { fooC =>
            assert(initialFooC.id == fooC.id, "Subsequent value does not match initial key")
            effects += Effect(s"update-child-${initialFooC.id}", s"FooC(${fooC.id}-${fooC.version})")
          }(owner)
          Bar(initialFooC.id)
        }
        .handleValue(FooO) {
          effects += Effect(s"init-child-${FooO.id}", s"FooO(${FooO.id})")
          Bar(FooO.id)
        }
        .toSignal

      signal.foreach { result =>
        effects += Effect("result", result.toString)
      }(owner)

      effects shouldBe mutable.Buffer(
        Effect("init-child-initial", "FooC(initial-1)"),
        Effect("update-child-initial", "FooC(initial-1)"),
        Effect("init-child-0", "FooE(0)"),
        Effect("update-child-0", "FooE(0)"),
        Effect("init-child-object", "FooO(object)"),
        Effect("result", "List(Bar(initial), Bar(0), Bar(object))")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooO :: FooC("a", 1) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child-a", "FooC(a-1)"),
        Effect("update-child-a", "FooC(a-1)"),
        Effect("result", "List(Bar(object), Bar(a))")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("a", 2) :: FooO :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(a), Bar(object))"),
        Effect("update-child-a", "FooC(a-2)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooE.FooE2 :: FooC("a", 3) :: FooE.FooE3 :: FooC("b", 1) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child-1", "FooE(1)"),
        Effect("update-child-1", "FooE(1)"),
        Effect("init-child--1", "FooE(-1)"),
        Effect("update-child--1", "FooE(-1)"),
        Effect("init-child-b", "FooC(b-1)"),
        Effect("update-child-b", "FooC(b-1)"),
        Effect("result", "List(Bar(1), Bar(a), Bar(-1), Bar(b))"),
        Effect("update-child-a", "FooC(a-3)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("b", 1) :: FooC("a", 3) :: FooE.FooE2 :: FooE.FooE3 :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(a), Bar(1), Bar(-1))"),
        Effect("update-child-a", "FooC(a-3)"),
        Effect("update-child-1", "FooE(1)"),
        Effect("update-child--1", "FooE(-1)"),
        Effect("update-child-b", "FooC(b-1)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooE.FooE2 :: FooC("b", 1) :: FooC("a", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(1), Bar(b), Bar(a))"),
        Effect("update-child-a", "FooC(a-4)"),
        Effect("update-child-1", "FooE(1)"),
        Effect("update-child-b", "FooC(b-1)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("b", 2) :: FooC("a", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(a))"),
        Effect("update-child-a", "FooC(a-4)"),
        Effect("update-child-b", "FooC(b-2)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("b", 3) :: FooC("a", 5) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(a))"),
        Effect("update-child-a", "FooC(a-5)"),
        Effect("update-child-b", "FooC(b-3)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("b", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b))"),
        Effect("update-child-b", "FooC(b-4)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("b", 4) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b))"),
        Effect("update-child-b", "FooC(b-4)")
      )
    }
  }

  it("split signal - raw semantics and life cycle") {
    withOrWithoutDuplicateKeyWarnings {
      val effects = mutable.Buffer[Effect[String]]()

      val myVar = Var[List[Foo]](FooC("initial", 1) :: FooO :: FooE.FooE1 :: Nil)

      val outerDynamicOwner = new DynamicOwner(() => throw new Exception("split outer dynamic owner accessed after it was killed"))

      val innerDynamicOwnerE = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

      val innerDynamicOwnerC = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

      val innerDynamicOwnerO = new DynamicOwner(() => throw new Exception("split inner dynamic owner accessed after it was killed"))

      // #Note: important to activate now, we're testing this (see comments below)
      outerDynamicOwner.activate()
      innerDynamicOwnerE.activate()
      innerDynamicOwnerC.activate()
      innerDynamicOwnerO.activate()

      // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
      //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
      val signal = myVar.signal.splitMatchSeq(_.id, distinctCompose = identity)
        .handleCase {
          case FooE(Some(num)) => num
          case FooE(None) => -1
        } { (initialNum, numSignal) =>
          effects += Effect(s"init-child-$initialNum", s"FooE($initialNum)")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          Transaction { _ =>
            DynamicSubscription.subscribeCallback(
              innerDynamicOwnerE,
              owner => numSignal.foreach { num =>
                assert(initialNum == num, "Subsequent value does not match initial key")
                effects += Effect(s"update-child-$num", s"FooE($num)")
              }(owner)
            )
          }
          Bar(s"$initialNum")
        }
        .handleType[FooC] { (initialFooC, fooCSignal) =>
          effects += Effect(s"init-child-${initialFooC.id}", s"FooC(${initialFooC.id}-${initialFooC.version})")
          // @Note keep foreach or addObserver here – this is important.
          //  It tests that SplitSignal does not cause an infinite loop trying to evaluate its initialValue.
          DynamicSubscription.subscribeCallback(
            innerDynamicOwnerC,
            owner => fooCSignal.foreach { fooC =>
              assert(initialFooC.id == fooC.id, "Subsequent value does not match initial key")
              effects += Effect(s"update-child-${initialFooC.id}", s"FooC(${fooC.id}-${fooC.version})")
            }(owner)
          )
          Bar(initialFooC.id)
        }
        .handleValue(FooO) {
          effects += Effect(s"init-child-${FooO.id}", s"FooO(${FooO.id})")
          Bar(FooO.id)
        }
        .toSignal

      DynamicSubscription.subscribeCallback(
        outerDynamicOwner,
        owner => signal.foreach { result =>
          effects += Effect("result", result.toString)
        }(owner)
      )

      effects shouldBe mutable.Buffer(
        Effect("init-child-initial", "FooC(initial-1)"),
        Effect("update-child-initial", "FooC(initial-1)"),
        Effect("init-child-object", "FooO(object)"),
        Effect("init-child-0", "FooE(0)"),
        Effect("result", "List(Bar(initial), Bar(object), Bar(0))"),
        Effect("update-child-0", "FooE(0)")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooE.FooE1 :: FooC("b", 1) :: FooO :: FooC("a", 3) :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child-b", "FooC(b-1)"),
        Effect("update-child-b", "FooC(b-1)"),
        Effect("init-child-a", "FooC(a-3)"),
        Effect("update-child-a", "FooC(a-3)"),
        Effect("result", "List(Bar(0), Bar(b), Bar(object), Bar(a))"),
        Effect("update-child-0", "FooE(0)")
      )

      effects.clear()

      // --

      outerDynamicOwner.deactivate()
      innerDynamicOwnerO.deactivate()
      innerDynamicOwnerE.deactivate()
      innerDynamicOwnerC.deactivate()

      effects shouldBe mutable.Buffer()

      // --

      outerDynamicOwner.activate()
      innerDynamicOwnerC.activate()

      effects shouldBe mutable.Buffer(
        // #Note `initial` is here because our code created an inner subscription for `initial`
        //  and kept it alive even after the element was removed. This inner signal itself will
        //  not receive any updates until "initial" key is added to the inputs again (actually
        //  it might cause issues in this pattern if this happens), but this inner signal's
        //  current value is still sent to the observer when we re-activate its dynamic owner.
        Effect("result", "List(Bar(0), Bar(b), Bar(object), Bar(a))"),
        Effect("update-child-initial", "FooC(initial-1)"),
        Effect("update-child-b", "FooC(b-1)"),
        Effect("update-child-a", "FooC(a-3)"),
      )

      effects.clear()

      // --

      innerDynamicOwnerO.activate()

      effects shouldBe mutable.Buffer()

      // --

      innerDynamicOwnerE.activate()

      effects shouldBe mutable.Buffer(
        Effect("update-child-0", "FooE(0)")
      )

      // --

      effects.clear()

      myVar.writer.onNext(FooC("b", 2) :: FooC("a", 3) :: FooO :: Nil)

      // This assertion makes sure that `resetOnStop` is set correctly in `drop(1, resetOnStop = false)`
      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(a), Bar(object))"),
        Effect("update-child-b", "FooC(b-2)"),
        Effect("update-child-a", "FooC(a-3)")
      )

      effects.clear()

      // --

      outerDynamicOwner.deactivate()

      effects shouldBe mutable.Buffer()

      // --

      outerDynamicOwner.activate()

      effects shouldBe mutable.Buffer(
        Effect("result", "List(Bar(b), Bar(a), Bar(object))")
      )

      effects.clear()

      // --

      myVar.writer.onNext(FooC("a", 4) :: FooC("b", 3) :: FooE.FooE2 :: Nil)

      effects shouldBe mutable.Buffer(
        Effect("init-child-1", "FooE(1)"),
        Effect("result", "List(Bar(a), Bar(b), Bar(1))"),
        Effect("update-child-b", "FooC(b-3)"),
        Effect("update-child-a", "FooC(a-4)"),
        Effect("update-child-1", "FooE(1)")
      )

      //effects.clear()
    }
  }

  it("split signal - duplicate keys") {

    val myVar = Var[List[Foo]](FooC("initial", 1) :: Nil)

    val owner = new TestableOwner

    // #Note: `identity` here means we're not using `distinct` to filter out redundancies in fooSignal
    //  We test like this to make sure that the underlying splitting machinery works correctly without this crutch
    val signal = myVar.signal.splitMatchSeq(_.id, distinctCompose = identity)
        .handleCase {
          case FooE(Some(num)) => num
          case FooE(None) => -1
        } { (initialNum, _) =>
          Bar(s"$initialNum")
        }
        .handleType[FooC] { (initialFooC, _) =>
          Bar(initialFooC.id)
        }
        .handleValue(FooO) {
          Bar(FooO.id)
        }
        .toSignal

    // --

    signal.addObserver(Observer.empty)(owner)

    // --

    myVar.writer.onNext(FooC("object", 1) :: FooC("int_", 1) :: FooO :: FooE.FooE2 :: Nil)

    // --

    DuplicateKeysConfig.setDefault(DuplicateKeysConfig.noWarnings)

    myVar.writer.onNext(FooC("object", 1) :: FooC("object", 1) :: FooC("int_", 1) :: FooO :: FooE.FooE2 :: FooO :: Nil)

    // This should warn, but not throw

    // #TODO[Test] we aren't actually testing that this is logging to the console.
    //  I'm not sure how to do this without over-complicating things.
    //  The console warning is printed into the test output, we can at least see it there if / when we look

    DuplicateKeysConfig.setDefault(DuplicateKeysConfig.warnings)

    myVar.writer.onNext(FooC("object", 1) :: FooC("int_", 1) :: FooO :: FooE.FooE2 :: Nil)

    myVar.writer.onNext(FooC("object", 1) :: FooC("object", 1) :: FooC("int_", 1) :: FooO :: FooE.FooE2 :: FooO :: Nil)
  }

  it("split list / vector / set / js.array / immutable.seq / collection.seq / option compiles") {
    // Having this test pass on all supported Scala versions is important to ensure that the implicits are actually usable.
    {
      (new EventBus[List[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[Vector[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[Set[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[js.Array[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[JsArray[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[JsVector[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[immutable.Seq[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[collection.Seq[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[collection.Seq[Foo]]).events.splitMatchSeq(_.id).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
    }
    // And now the same, but with `distinctCompose = identity`, because that somehow affects implicit resolution in Scala 3.0.0
    {
      (new EventBus[List[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[Vector[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[Set[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[js.Array[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[JsArray[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[JsVector[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[immutable.Seq[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[collection.Seq[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
      (new EventBus[collection.Seq[Foo]]).events.splitMatchSeq(_.id, identity).handleCase{ case e: FooE => e }((_, _) => 10).handleType[FooC]((_, _) => 20).handleValue(FooO)(30).toSignal
    }
  }

}
