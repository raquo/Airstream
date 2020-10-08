package com.raquo.airstream.signal

import com.raquo.airstream.UnitSpec
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
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    errorEffects.clear()
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

    val sub2 = signal.addObserver(obs)(owner)

    // Emit a value to the new external observer. Standard Signal behaviour.
    assert(calculations == mutable.Buffer())
    assert(effects == mutable.Buffer(Effect("obs", 3)))

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

    val sub1 = signal.addObserver(obs)(owner)

    // Error values are propagated to new observers
    assert(errorEffects == mutable.Buffer(Effect("signal-err", err1)))

    errorEffects.clear()

    // --

    x.tryUpdate(_ => Failure(err2))

    assert(x.tryNow() == Failure(err2))
    assert(errorEffects == mutable.Buffer(Effect("signal-err", err2)))

    errorEffects.clear()

    // --

    // Similar to .now(), doing .update() on an errored Var throws
    assert(Try(x.update(_ + 1)).isFailure)

    // We are unable to update a value if the current value is an error
    assert(x.tryNow() == Failure(err2))
    assert(errorEffects == mutable.Buffer())

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

    lazy val err1 = new Exception("err1")
    lazy val err2 = new Exception("err2")

    Var.set(x -> 2, y -> 200)

    assert(x.now() == 2)
    assert(y.now() == 200)

    // --

    Var.setTry(x -> Failure(err1), y -> Success(300))

    assert(x.tryNow() == Failure(err1))
    assert(y.now() == 300)

    // --

    // @TODO[API] Figure out if there is an elegant solution that would allow for type inference here
    val result = Try(Var.update(
      x -> ((_: Int) => 4),
      y -> ((curr: Int) => curr + 100)
    ))

    // Can't update 'x' because it's failed.
    // Both updates will fail because of atomicity. All Vars will retain their previous values.
    assert(x.tryNow() == Failure(err1))
    assert(y.now() == 300)
    assert(result.isFailure)

    // --

    // Same as above but ordered differently
    val result2 = Try(Var.update(
      y -> ((curr: Int) => curr + 100),
      x -> ((_: Int) => 4)
    ))

    // Can't update 'x' because it's failed.
    // Both updates will fail because of atomicity. All Vars will retain their previous values.
    assert(x.tryNow() == Failure(err1))
    assert(y.now() == 300)
    assert(result2.isFailure)

    // --

    Var.tryUpdate(
      x -> ((_: Try[Int]) => Success(5)),
      y -> ((_: Try[Int]) => Failure(err2))
    )

    assert(x.now() == 5)
    assert(y.tryNow() == Failure(err2))
  }

  it("batch updates are glitch-free") {

    val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer[Int](effects += Effect("obs", _))

    val x = Var(1)
    val y = Var(100)

    val sumSignal = x.signal.combineWith(y.signal).map2(_ + _).map(Calculation.log("signal", calculations))

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
    // If we allowed this, you would be able to send two events into the same Var
    // in the same transaction, which breaks Airstream contract.
    val var1 = Var(0)
    val var2 = Var(0)
    val var3 = Var(0)

    // -- should not fail

    Var.setTry(
      var1 -> Success(1),
      var2 -> Success(1),
      var3 -> Failure(new Exception("Var 3 is broken"))
    )

    Var.set(
      var1 -> 1,
      var2 -> 1,
      var3 -> 1
    )

    // --

    Try(Var.set(
      var1 -> 2,
      var2 -> 2,
      var1 -> 2
    )).isFailure shouldBe true

    // --

    Try(Var.setTry(
      var1 -> Success(3),
      var2 -> Success(4),
      var2 -> Success(5)
    )).isFailure shouldBe true

    // --

    Try(Var.update(
      var1 -> ((_: Int) + 1),
      var2 -> ((_: Int) + 2),
      var2 -> ((_: Int) + 3)
    )).isFailure shouldBe true

    // --

    Try(Var.tryUpdate(
      var1 -> ((_: Try[Int]).map(_ + 1)),
      var2 -> ((_: Try[Int]).map(_ + 2)),
      var2 -> ((_: Try[Int]).map(_ + 3))
    )).isFailure shouldBe true

    // --

    var1.now() shouldBe 1
    var2.now() shouldBe 1
    var3.now() shouldBe 1
  }
}
