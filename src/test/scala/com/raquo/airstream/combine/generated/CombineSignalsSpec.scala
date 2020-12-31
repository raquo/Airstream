package com.raquo.airstream.combine.generated

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.fixtures.TestableOwner
import com.raquo.airstream.signal.{Signal, Var}

import scala.collection.mutable

class CombineSignalsSpec extends UnitSpec {

  case class T1(v: Int) { def inc: T1 = T1(v+1) }
  case class T2(v: Int) { def inc: T2 = T2(v+1) }
  case class T3(v: Int) { def inc: T3 = T3(v+1) }
  case class T4(v: Int) { def inc: T4 = T4(v+1) }
  case class T5(v: Int) { def inc: T5 = T5(v+1) }
  case class T6(v: Int) { def inc: T6 = T6(v+1) }
  case class T7(v: Int) { def inc: T7 = T7(v+1) }
  case class T8(v: Int) { def inc: T8 = T8(v+1) }
  case class T9(v: Int) { def inc: T9 = T9(v+1) }
  case class T10(v: Int) { def inc: T10 = T10(v+1) }

  it("CombineSignal2 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal)

    val effects = mutable.Buffer[(T1, T2)]()

    val observer = Observer[(T1, T2)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal3 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal)

    val effects = mutable.Buffer[(T1, T2, T3)]()

    val observer = Observer[(T1, T2, T3)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal4 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))
    val var4 = Var(T4(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal, var4.signal)

    val effects = mutable.Buffer[(T1, T2, T3, T4)]()

    val observer = Observer[(T1, T2, T3, T4)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1), T4(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      var4.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration), T4(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration), T4(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal5 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))
    val var4 = Var(T4(1))
    val var5 = Var(T5(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal, var4.signal, var5.signal)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5)]()

    val observer = Observer[(T1, T2, T3, T4, T5)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1), T4(1), T5(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      var4.update(_.inc)
      var5.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration), T5(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal6 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))
    val var4 = Var(T4(1))
    val var5 = Var(T5(1))
    val var6 = Var(T6(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal, var4.signal, var5.signal, var6.signal)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1), T4(1), T5(1), T6(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      var4.update(_.inc)
      var5.update(_.inc)
      var6.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration), T6(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal7 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))
    val var4 = Var(T4(1))
    val var5 = Var(T5(1))
    val var6 = Var(T6(1))
    val var7 = Var(T7(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal, var4.signal, var5.signal, var6.signal, var7.signal)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6, T7)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6, T7)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1), T4(1), T5(1), T6(1), T7(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      var4.update(_.inc)
      var5.update(_.inc)
      var6.update(_.inc)
      var7.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration), T7(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal8 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))
    val var4 = Var(T4(1))
    val var5 = Var(T5(1))
    val var6 = Var(T6(1))
    val var7 = Var(T7(1))
    val var8 = Var(T8(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal, var4.signal, var5.signal, var6.signal, var7.signal, var8.signal)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6, T7, T8)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6, T7, T8)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1), T4(1), T5(1), T6(1), T7(1), T8(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      var4.update(_.inc)
      var5.update(_.inc)
      var6.update(_.inc)
      var7.update(_.inc)
      var8.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration), T8(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal9 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))
    val var4 = Var(T4(1))
    val var5 = Var(T5(1))
    val var6 = Var(T6(1))
    val var7 = Var(T7(1))
    val var8 = Var(T8(1))
    val var9 = Var(T9(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal, var4.signal, var5.signal, var6.signal, var7.signal, var8.signal, var9.signal)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6, T7, T8, T9)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1), T4(1), T5(1), T6(1), T7(1), T8(1), T9(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      var4.update(_.inc)
      var5.update(_.inc)
      var6.update(_.inc)
      var7.update(_.inc)
      var8.update(_.inc)
      var9.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration + 1), T9(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration + 1), T9(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }

  it("CombineSignal10 works") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val var1 = Var(T1(1))
    val var2 = Var(T2(1))
    val var3 = Var(T3(1))
    val var4 = Var(T4(1))
    val var5 = Var(T5(1))
    val var6 = Var(T6(1))
    val var7 = Var(T7(1))
    val var8 = Var(T8(1))
    val var9 = Var(T9(1))
    val var10 = Var(T10(1))

    val combinedSignal = Signal.combine(var1.signal, var2.signal, var3.signal, var4.signal, var5.signal, var6.signal, var7.signal, var8.signal, var9.signal, var10.signal)

    val effects = mutable.Buffer[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)]()

    val observer = Observer[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)](effects += _)

    // --

    effects.toList shouldBe empty

    // --

    val subscription = combinedSignal.addObserver(observer)

    // --

    effects.toList should ===(List(
      (T1(1), T2(1), T3(1), T4(1), T5(1), T6(1), T7(1), T8(1), T9(1), T10(1))
    ))

    // --

    for (iteration <- 0 until 10) {
      effects.clear()
      var1.update(_.inc)
      var2.update(_.inc)
      var3.update(_.inc)
      var4.update(_.inc)
      var5.update(_.inc)
      var6.update(_.inc)
      var7.update(_.inc)
      var8.update(_.inc)
      var9.update(_.inc)
      var10.update(_.inc)
      effects.toList should ===(
        List(
          (T1(1 + iteration + 1), T2(1 + iteration), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration), T8(1 + iteration), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration + 1), T9(1 + iteration), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration + 1), T9(1 + iteration + 1), T10(1 + iteration)),
          (T1(1 + iteration + 1), T2(1 + iteration + 1), T3(1 + iteration + 1), T4(1 + iteration + 1), T5(1 + iteration + 1), T6(1 + iteration + 1), T7(1 + iteration + 1), T8(1 + iteration + 1), T9(1 + iteration + 1), T10(1 + iteration + 1))
        )
      )
    }

    subscription.kill()
  }


}
