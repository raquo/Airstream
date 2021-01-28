package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class DerivedVarSpec extends UnitSpec with BeforeAndAfter {

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

  it("error handling") {

    val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val obs = Observer[Int](effects += Effect("obs", _))

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
    //assert(errorEffects == mutable.Buffer(Effect("signal-err", err1)))

    errorEffects.clear()

    // --

    x.tryUpdate(_ => Failure(err2))

    assert(x.tryNow() == Failure(err2))
    //assert(errorEffects == mutable.Buffer(Effect("signal-err", err2)))

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
    //assert(errorEffects == mutable.Buffer(Effect("signal-err", err3)))

    errorEffects.clear()
  }

  it("strict updates") {

    val owner = new TestableOwner

    val s = Var(10)
    val d = s.zoom(_ + 100)(_ - 100)(owner)

    assert(s.tryNow() == Success(10))
    assert(s.now() == 10)
    assert(s.signal.now() == 10)

    assert(d.tryNow() == Success(110))
    assert(d.now() == 110)
    assert(d.signal.now() == 110)

    // --

    d.writer.onNext(120)

    assert(s.tryNow() == Success(20))
    assert(s.now() == 20)
    assert(s.signal.now() == 20)

    assert(d.tryNow() == Success(120))
    assert(d.now() == 120)
    assert(d.signal.now() == 120)

    // --

    d.update(_ + 1)

    assert(s.now() == 21)
    assert(s.signal.now() == 21)

    assert(d.now() == 121)
    assert(d.signal.now() == 121)

    // --

    d.tryUpdate(currTry => currTry.map(_ + 1))

    assert(s.now() == 22)
    assert(s.signal.now() == 22)

    assert(d.now() == 122)
    assert(d.signal.now() == 122)

    // --

    s.writer.onNext(30)

    assert(s.tryNow() == Success(30))
    assert(s.now() == 30)
    assert(s.signal.now() == 30)

    assert(d.tryNow() == Success(130))
    assert(d.now() == 130)
    assert(d.signal.now() == 130)

    // --

    s.update(_ + 1)

    assert(s.tryNow() == Success(31))
    assert(s.now() == 31)
    assert(s.signal.now() == 31)

    assert(d.tryNow() == Success(131))
    assert(d.now() == 131)
    assert(d.signal.now() == 131)

    // --

    s.tryUpdate(currTry => currTry.map(_ + 1))

    assert(s.now() == 32)
    assert(s.signal.now() == 32)

    assert(d.now() == 132)
    assert(d.signal.now() == 132)
  }

  it("laziness, updates and errors") {

    val varOwner = new TestableOwner
    val obsOwner = new TestableOwner

    val s = Var(10)
    val d = s.zoom(_ + 100)(_ - 100)(varOwner)

    val err1 = new Exception("Error: err1")

    assert(s.tryNow() == Success(10))
    assert(s.now() == 10)
    assert(s.signal.now() == 10)

    assert(d.tryNow() == Success(110))
    assert(d.now() == 110)
    assert(d.signal.now() == 110)

    // -- Errors propagate

    s.setTry(Failure(err1))

    assert(s.tryNow() == Failure(err1))

    assert(d.tryNow() == Failure(err1))

    assert(errorEffects.toList == List(
      Effect("unhandled", err1)
    ))

    errorEffects.clear()

    // -- Can't update a failed var

    d.update(_ + 1)

    d.tryNow() shouldBe Failure(err1)
    // Remember, a Var without a listener does emit its errors into "unhandled"
    errorEffects shouldBe mutable.Buffer(
      Effect("unhandled", VarError("Unable to update a failed Var. Consider Var#tryUpdate instead.", cause = Some(err1)))
    )
    errorEffects.clear()

    // -- Can't update the same underlying var twice

    assert(Try(Var.set(
      s -> 1,
      d -> 2
    )).isFailure)

    // -- Restore normality

    d.set(101)

    assert(s.tryNow() == Success(1))
    assert(s.now() == 1)
    assert(s.signal.now() == 1)

    assert(d.tryNow() == Success(101))
    assert(d.now() == 101)
    assert(d.signal.now() == 101)

    // --

    val effects = mutable.Buffer[Effect[Int]]()
    val obs = Observer[Int](effects += Effect("obs", _))

    d.signal.addObserver(obs)(obsOwner)

    varOwner.killSubscriptions() // we're specifically killing the owner that updates the derived var

    assert(effects.toList == List(Effect("obs", 101)))

    effects.clear()

    // --

    // Derived var is still active because of obsOwner, even though varOwner is dead already.
    // This is consistent with StrictSignal behaviour.

    d.set(102)

    assert(s.tryNow() == Success(2))
    assert(s.now() == 2)
    assert(s.signal.now() == 2)

    assert(d.tryNow() == Success(102))
    assert(d.now() == 102)
    assert(d.signal.now() == 102)

    assert(effects.toList == List(Effect("obs", 102)))

    effects.clear()

    // --

    obsOwner.killSubscriptions()

    assert(effects.isEmpty)

    // Now the derived var is killed

    // --

    d.set(134)

    // See comments in DerivedVar regarding error behaviour

    assert(s.now() == 2)
    assert(s.signal.now() == 2)

    assert(d.now() == 102)
    assert(d.signal.now() == 102)

    assert(errorEffects.toList == List(
      Effect("unhandled", VarError("Unable to set current value Success(134) on inactive derived var", None))
    ))

    errorEffects.clear()
  }


  it("signal does not glitch") {

    val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val s = Var(1)
    val d = s.zoom(_ + 100)(_ - 100)(owner)

    val combinedSignal = d.signal.combineWithFn(s.signal)(_ * 1000 + _) // e.g. if s.now() is 2, this is 102002

    val sourceObs = Observer[Int](effects += Effect("source-obs", _))
    val derivedObs = Observer[Int](effects += Effect("derived-obs", _))
    val combinedObs = Observer[Int](effects += Effect("combined-obs", _))

    assert(s.tryNow() == Success(1))
    assert(s.now() == 1)
    assert(s.signal.now() == 1)

    assert(d.tryNow() == Success(101))
    assert(d.now() == 101)
    assert(d.signal.now() == 101)

    assert(calculations == mutable.Buffer())

    // --

    s.set(2)

    assert(s.tryNow() == Success(2))
    assert(s.now() == 2)
    assert(s.signal.now() == 2)

    assert(d.tryNow() == Success(102))
    assert(d.now() == 102)
    assert(d.signal.now() == 102)

    assert(calculations == mutable.Buffer())
    assert(effects == mutable.Buffer())

    // --

    // #Note observers are added to Var signals directly
    s.signal.addObserver(sourceObs)(owner)
    d.signal.addObserver(derivedObs)(owner)
    combinedSignal.addObserver(combinedObs)(owner)

    assert(effects.toList == List(
      Effect("source-obs", 2),
      Effect("derived-obs", 102),
      Effect("combined-obs", 102002),
    ))

    calculations.clear()
    effects.clear()

    // --

    // @TODO Why does the order of calculations (source vs derived) switch here?
    //  - Probably something to do with previous calculation being the first one after observers were added?
    //  - Our contract doesn't really mandate any order since these are derived MapSignal-s, not

    s.set(3)

    assert(effects.toList == List(
      Effect("source-obs", 3),
      Effect("derived-obs", 103),
      Effect("combined-obs", 103003)
    ))

    calculations.clear()
    effects.clear()

    // --

    d.set(104)

    assert(effects.toList == List(
      Effect("source-obs", 4),
      Effect("derived-obs", 104),
      Effect("combined-obs", 104004)
    ))

    calculations.clear()
    effects.clear()

    // --

    val derivedAdder = d.updater[Int](_ + _)

    derivedAdder.onNext(10)

    assert(effects.toList == List(
      Effect("source-obs", 14),
      Effect("derived-obs", 114),
      Effect("combined-obs", 114014)
    ))
  }
}
