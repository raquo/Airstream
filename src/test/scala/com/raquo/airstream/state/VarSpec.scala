package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class VarSpec extends UnitSpec with BeforeAndAfter {

  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  private val errorCallback = (err: Throwable) => {
    errorEffects += Effect("unhandled", err)
    ()
  }

  before {
    errorEffects.clear()
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    assert(errorEffects.isEmpty) // #Note this fails the test rather inelegantly
  }

  it("strict updates") {

    //    val owner = new TestableOwner

    val x = Var(10)

    assert(x.tryNow() == Success(10))
    assert(x.now() == 10)
    assert(x.signal.now() == 10)

    // --

    x.writer.onNext(20)

    assert(x.tryNow() == Success(20))
    assert(x.now() == 20)
    assert(x.signal.now() == 20)

    // --

    x.update(_ + 1)

    assert(x.now() == 21)
    assert(x.signal.now() == 21)

    // --

    x.tryUpdate(currTry => currTry.map(_ + 1))

    assert(x.now() == 22)
    assert(x.signal.now() == 22)
  }

  it("signal propagation") {

    val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer[Int](effects += Effect("obs", _))

    val x = Var(1)

    val signal = x.signal.map(Calculation.log("signal", calculations))

    assert(x.tryNow() == Success(1))
    assert(x.now() == 1)
    assert(x.signal.now() == 1)
    assert(calculations == mutable.Buffer())

    // --

    x.writer.onNext(2)

    assert(x.tryNow() == Success(2))
    assert(x.now() == 2)
    assert(x.signal.now() == 2)
    assert(calculations == mutable.Buffer())
    assert(effects == mutable.Buffer())

    // --

    val sub1 = signal.addObserver(obs)(owner)

    assert(calculations == mutable.Buffer(Calculation("signal", 2)))
    assert(effects == mutable.Buffer(Effect("obs", 2)))

    calculations.clear()
    effects.clear()

    // --

    x.writer.onNext(3)

    assert(calculations == mutable.Buffer(Calculation("signal", 3)))
    assert(effects == mutable.Buffer(Effect("obs", 3)))

    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    x.writer.onNext(4)

    signal.addObserver(obs)(owner)

    // Re-sync the value and emit it to the new external observer. Standard Signal behaviour.
    assert(calculations == mutable.Buffer(Calculation("signal", 4)))
    assert(effects == mutable.Buffer(Effect("obs", 4)))

    calculations.clear()
    effects.clear()

    // --

    x.writer.onNext(5)

    assert(calculations == mutable.Buffer(Calculation("signal", 5)))
    assert(effects == mutable.Buffer(Effect("obs", 5)))

    calculations.clear()
    effects.clear()
  }

  it("error handling") {

    val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer.withRecover[Int](effects += Effect("obs", _), {
      case err => errorEffects += Effect("signal-err", err)
    })

    val x = Var(1)

    val signal = x.signal.map(Calculation.log("signal", calculations))

    lazy val err1 = new Exception("err1")
    lazy val err2 = new Exception("err2")
    lazy val err3 = new Exception("err3")

    x.writer.onError(err1)

    assert(x.tryNow() == Failure(err1))
    assert(errorEffects == mutable.Buffer(Effect("unhandled", err1)))

    errorEffects.clear()

    // --

    signal.addObserver(obs)(owner)

    // Error values are propagated to new observers
    assert(errorEffects == mutable.Buffer(Effect("signal-err", err1)))

    errorEffects.clear()

    // --

    x.tryUpdate(_ => Failure(err2))

    assert(x.tryNow() == Failure(err2))
    assert(errorEffects == mutable.Buffer(Effect("signal-err", err2)))

    errorEffects.clear()

    // --

    x.update(_ + 1)

    // We are unable to update a value if the current value is an error
    assert(x.tryNow() == Failure(err2))
    assert(errorEffects == mutable.Buffer(
      Effect("unhandled", VarError("Unable to update a failed Var. Consider Var#tryUpdate instead.", cause = Some(err2)))
    ))

    errorEffects.clear()

    // --

    x.writer.onNext(10)

    assert(x.now() == 10)
    assert(errorEffects == mutable.Buffer())

    // --

    x.update(_ => throw err3)

    assert(x.tryNow() == Failure(err3))
    assert(errorEffects == mutable.Buffer(Effect("signal-err", err3)))

    errorEffects.clear()
  }

  it("batch updates") {

    val x = Var(1)
    val y = Var(100)
    val z = Var("")

    lazy val err1 = new Exception("err1")
    lazy val err2 = new Exception("err2")

    Var.set(x -> 2, y -> 200, z -> "z")

    assert(x.now() == 2)
    assert(y.now() == 200)
    assert(z.now() == "z")

    // --

    Var.setTry(x -> Failure(err1), y -> Success(300))

    assert(x.tryNow() == Failure(err1))
    assert(y.now() == 300)

    assert(errorEffects.toList == List(
      Effect("unhandled", err1)
    ))

    errorEffects.clear()

    // --

    // @TODO[API] Figure out if there is an elegant solution that would allow for type inference here
    val result = Try(Var.update(
      x -> ((_: Int) => 4),
      y -> ((curr: Int) => curr + 100),
      z -> ((curr: String) => curr + "_")
    ))

    // Can't update 'x' because it's failed.
    // All batched updates will fail because of atomicity. All Vars will retain their previous values.
    assert(x.tryNow() == Failure(err1))
    assert(y.now() == 300)
    assert(z.now() == "z")
    assert(result.isFailure)

    // --

    // Same as above but ordered differently, so that the failing update is last.
    val result2 = Try(Var.update(
      y -> ((curr: Int) => curr + 100),
      z -> ((curr: String) => curr + "a"),
      x -> ((_: Int) => 4)
    ))

    // Can't update 'x' because it's failed.
    // Both updates will fail because of atomicity. All Vars will retain their previous values.
    assert(x.tryNow() == Failure(err1))
    assert(y.now() == 300)
    assert(z.now() == "z")
    assert(result2.isFailure)

    // --

    Var.tryUpdate(
      x -> ((_: Try[Int]) => Success(5)),
      y -> ((_: Try[Int]) => Failure(err2)),
      z -> ((curr: Try[String]) => curr.map(_ + "a"))
    )

    assert(x.now() == 5)
    assert(y.tryNow() == Failure(err2))
    assert(z.now() == "za")

    assert(errorEffects.toList == List(
      Effect("unhandled", err2)
    ))

    errorEffects.clear()
  }

  it("batch updates are glitch-free") {

    val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer[Int](effects += Effect("obs", _))

    val x = Var(1)
    val y = Var(100)

    val sumSignal = x.signal.combineWith(y.signal).mapN(_ + _).map(Calculation.log("signal", calculations))

    assert(calculations == mutable.Buffer())
    assert(effects == mutable.Buffer())

    // --

    sumSignal.addObserver(obs)(owner)

    assert(calculations == mutable.Buffer(Calculation("signal", 101)))
    assert(effects == mutable.Buffer(Effect("obs", 101)))

    calculations.clear()
    effects.clear()

    // --

    Var.set(x -> 2, y -> 200)

    assert(calculations == mutable.Buffer(Calculation("signal", 202)))
    assert(effects == mutable.Buffer(Effect("obs", 202)))

    calculations.clear()
    effects.clear()
  }

  it("disallow duplicate vars in Var.set and such") {

    val owner = new TestableOwner

    // If we allowed this, you would be able to send two events into the same Var
    // in the same transaction, which breaks Airstream contract.
    val var1 = Var(0)
    val var2 = Var(0)
    val var3 = Var(0)
    val zoomedVar1 = var1.zoom(identity)(identity)(owner)
    val zoomedVar2 = var2.zoom(identity)(identity)(owner)

    val err3 = new Exception("Var 3 is broken")

    // -- should not fail

    Var.setTry(
      var1 -> Success(1),
      var2 -> Success(1),
      var3 -> Failure(err3)
    )

    Var.set(
      var1 -> 1,
      var2 -> 1,
      var3 -> 1
    )

    assert(errorEffects.toList == List(
      Effect("unhandled", err3)
    ))

    errorEffects.clear()

    // --

    Try(Var.set(
      var1 -> 2,
      var2 -> 2,
      var1 -> 2
    )).isFailure shouldBe true

    // --

    Try(Var.set(
      var1 -> 2,
      var2 -> 2,
      zoomedVar1 -> 2
    )).isFailure shouldBe true

    // --

    Try(Var.setTry(
      var1 -> Success(3),
      var2 -> Success(4),
      var2 -> Success(5)
    )).isFailure shouldBe true

    Try(Var.setTry(
      var1 -> Success(3),
      zoomedVar2 -> Success(4),
      var2 -> Success(5)
    )).isFailure shouldBe true

    // --

    Try(Var.update(
      var1 -> ((_: Int) + 1),
      var2 -> ((_: Int) + 2),
      var2 -> ((_: Int) + 3)
    )).isFailure shouldBe true

    Try(Var.update(
      var1 -> ((_: Int) + 1),
      var2 -> ((_: Int) + 2),
      zoomedVar2 -> ((_: Int) + 3)
    )).isFailure shouldBe true

    // --

    Try(Var.tryUpdate(
      var1 -> ((_: Try[Int]).map(_ + 1)),
      var2 -> ((_: Try[Int]).map(_ + 2)),
      var2 -> ((_: Try[Int]).map(_ + 3))
    )).isFailure shouldBe true

    Try(Var.tryUpdate(
      var1 -> ((_: Try[Int]).map(_ + 1)),
      var2 -> ((_: Try[Int]).map(_ + 2)),
      zoomedVar2 -> ((_: Try[Int]).map(_ + 3))
    )).isFailure shouldBe true

    // --

    var1.now() shouldBe 1
    var2.now() shouldBe 1
    var3.now() shouldBe 1

    zoomedVar1.now() shouldBe 1
    zoomedVar2.now() shouldBe 1
  }

  it("updating Var which is in a failed state") {
    val err = new Exception("Known Var error 1")
    val v = Var(1)
    val v2 = Var(2)

    def reset(): Unit = {
      v.setTry(Failure(err))
      v2.set(2)
      v.tryNow() shouldBe Failure(err)
      v2.tryNow() shouldBe Success(2)

      assert(errorEffects.toList == List(
        Effect("unhandled", err)
      ))

      errorEffects.clear()
    }

    // --

    reset()

    // --

    v.set(-1)
    v.tryNow() shouldBe Success(-1)
    reset()

    // --

    v.setTry(Success(-11))
    v.tryNow() shouldBe Success(-11)
    reset()

    // --

    Var.set(
      v -> -10,
      v2 -> -20
    )
    v.tryNow() shouldBe Success(-10)
    v2.tryNow() shouldBe Success(-20)
    reset()

    // --

    Var.setTry(
      v -> Success(-1),
      v2 -> Success(-2)
    )
    v.tryNow() shouldBe Success(-1)
    v2.tryNow() shouldBe Success(-2)
    reset()

    // --

    v.update(_ * 10)
    v.tryNow() shouldBe Failure(err)
    // Remember, a Var without a listener does emit its errors into "unhandled"
    errorEffects shouldBe mutable.Buffer(
      Effect("unhandled", VarError("Unable to update a failed Var. Consider Var#tryUpdate instead.", cause = Some(err)))
    )
    errorEffects.clear()
    reset()

    // --

    v.tryUpdate(_.map(_ * 10))
    v.tryNow() shouldBe Failure(err)
    errorEffects.clear()
    reset()

    // --

    Try(Var.update(
      v -> ((_: Int) * 10),
      v2 -> ((_: Int) * 10)
    )).isFailure shouldBe true
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(2)
    errorEffects.clear()
    reset()

    // -- diff order

    Try(Var.update(
      v2 -> ((_: Int) * 10),
      v -> ((_: Int) * 10),
    )).isFailure shouldBe true
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(2)
    errorEffects.clear()
    reset()

    // --

    Var.tryUpdate(
      v -> ((_: Try[Int]).map(_ * 10)),
      v2 -> ((_: Try[Int]).map(_ * 10))
    )
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(20)
    errorEffects.clear()
    reset()

    // -- diff order

    Var.tryUpdate(
      v2 -> ((_: Try[Int]).map(_ * 10)),
      v -> ((_: Try[Int]).map(_ * 10))
    )
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(20)
    errorEffects.clear()
    reset()

  }

  it("updating Var with a failed value") {
    val err = new Exception("Known Var error 2")
    val v = Var(1)
    val v2 = Var(2)

    def reset(): Unit = {
      v.setTry(Failure(err))
      v2.set(2)
      v.tryNow() shouldBe Failure(err)
      v2.tryNow() shouldBe Success(2)

      assert(errorEffects.toList == List(
        Effect("unhandled", err)
      ))

      errorEffects.clear()
    }

    // --

    reset()

    // --

    v.setTry(Failure(err))
    v.tryNow() shouldBe Failure(err)
    errorEffects.clear()
    reset()

    // --

    Var.setTry(
      v -> Failure(err),
      v2 -> Success(-2)
    )
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(-2)
    errorEffects.clear()
    reset()

    // --

    v.tryUpdate(_ => Failure(err))
    v.tryNow() shouldBe Failure(err)
    errorEffects.clear()
    reset()

    // --

    Var.tryUpdate(
      v -> ((_: Try[Int]) => Failure(err)),
      v2 -> ((_: Try[Int]).map(_ * 10))
    )
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(20)
    errorEffects.clear()
    reset()

    // -- diff order

    Var.tryUpdate(
      v2 -> ((_: Try[Int]).map(_ * 10)),
      v -> ((_: Try[Int]) => Failure(err))
    )
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(20)
    errorEffects.clear()
    reset()

  }

  it("throwing in Var update mods") {
    val err = new Exception("Known Var error 3")
    val v = Var(1)
    val v2 = Var(2)

    def reset(): Unit = {
      v.set(1)
      v2.set(2)
      v.tryNow() shouldBe Success(1)
      v2.tryNow() shouldBe Success(2)
    }

    // --

    reset()

    // --

    v.update(_ => throw err)
    v.tryNow() shouldBe Failure(err)

    assert(errorEffects.toList == List(
      Effect("unhandled", err)
    ))

    errorEffects.clear()

    reset()

    // --

    Try(v.tryUpdate(_ => throw err)).isFailure shouldBe true
    v.tryNow() shouldBe Success(1)
    reset()

    assert(errorEffects.isEmpty)

    // --

    Var.update(
      v -> ((_: Int) => throw err),
      v2 -> ((_: Int) * 10)
    )
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(20)

    assert(errorEffects.toList == List(
      Effect("unhandled", err)
    ))

    errorEffects.clear()

    reset()

    // -- diff order

    Var.update(
      v2 -> ((_: Int) * 10),
      v -> ((_: Int) => throw err)
    )
    v.tryNow() shouldBe Failure(err)
    v2.tryNow() shouldBe Success(20)

    assert(errorEffects.toList == List(
      Effect("unhandled", err)
    ))

    errorEffects.clear()

    reset()

    // --

    Try(Var.tryUpdate(
      v -> ((_: Try[Int]) => throw err),
      v2 -> ((_: Try[Int]).map(_ * 10))
    )).isFailure shouldBe true
    v.tryNow() shouldBe Success(1)
    v2.tryNow() shouldBe Success(2)

    assert(errorEffects.isEmpty)

    reset()

    // -- diff order

    Try(Var.tryUpdate(
      v2 -> ((_: Try[Int]).map(_ * 10)),
      v -> ((_: Try[Int]) => throw err)
    )).isFailure shouldBe true
    v.tryNow() shouldBe Success(1)
    v2.tryNow() shouldBe Success(2)

    assert(errorEffects.isEmpty)

    reset()

  }

  it("updater") {

    val err1 = new Exception("err1")

    val err2 = new Exception("err2")

    val v = Var(List(1))

    val adder = v.updater[Int]((acc, newItem) => acc :+ newItem)

    val failedUpdater = v.updater[Int]((_, _) => throw err1)

    val doubler = v.updater[Unit]((acc, _) => acc ++ acc)

    // --

    v.now() shouldBe List(1)

    // --

    adder.onNext(2)

    v.now() shouldBe List(1, 2)

    // --

    adder.onError(err2)

    v.tryNow() shouldBe Failure(err2)

    // --

    adder.onNext(3)

    v.tryNow() shouldBe Failure(err2)

    // --

    v.set(List(0))

    v.now() shouldBe List(0)

    // --

    failedUpdater.onNext(1)

    v.tryNow() shouldBe Failure(err1)

    // --

    failedUpdater.onNext(1)

    v.tryNow() shouldBe Failure(err1)

    // --

    v.set(List(1, 2))
    doubler.onNext(())

    v.now() shouldBe List(1, 2, 1, 2)

    // @TODO[Test] Don't ignore, assert these properly
    errorEffects.clear()
  }

  it("tryUpdater") {

    val err1 = new Exception("err1")

    val resetErr = new Exception("resetErr")

    val v = Var(List(1))

    val adder = v.tryUpdater[Int] { (acc, newItem) =>
      acc
        .map(_ :+ newItem)
        .recover { case `resetErr` => List(0) }
    }

    val errer = v.tryUpdater[Int]((_, _) => Failure(resetErr))

    // --

    v.now() shouldBe List(1)

    // --

    adder.onNext(2)

    v.now() shouldBe List(1, 2)

    // --

    errer.onNext(3)

    v.tryNow() shouldBe Failure(resetErr)

    // --

    adder.onNext(1)

    v.tryNow() shouldBe Success(List(0))

    // --

    adder.onError(err1)

    v.tryNow() shouldBe Failure(err1)

    // --

    v.set(List(-1))

    v.now() shouldBe List(-1)

    // --

    errer.onNext(1)

    v.tryNow() shouldBe Failure(resetErr)

    // --

    errer.onNext(1)

    v.tryNow() shouldBe Failure(resetErr)

    // --

    v.set(List(1, 2))
    adder.onNext(3)

    v.now() shouldBe List(1, 2, 3)

    // @TODO[Test] Don't ignore, assert these properly
    errorEffects.clear()
  }
}
