package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class DerivedVarSpec extends UnitSpec with BeforeAndAfter {

  case class Form(int: Int)

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

    val owner = new TestableOwner

    val s = Var(Form(10))
    val d = s.zoom(_.int)((form, int) => form.copy(int = int))(owner)

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(d.tryNow() == Success(10))
    assert(d.now() == 10)
    assert(d.signal.now() == 10)

    // --

    d.writer.onNext(20)

    assert(s.tryNow() == Success(Form(20)))
    assert(s.now() == Form(20))
    assert(s.signal.now() == Form(20))

    assert(d.tryNow() == Success(20))
    assert(d.now() == 20)
    assert(d.signal.now() == 20)

    // --

    d.update(_ + 1)

    assert(s.now() == Form(21))
    assert(s.signal.now() == Form(21))

    assert(d.now() == 21)
    assert(d.signal.now() == 21)

    // --

    d.tryUpdate(currTry => currTry.map(_ + 1))

    assert(s.now() == Form(22))
    assert(s.signal.now() == Form(22))

    assert(d.now() == 22)
    assert(d.signal.now() == 22)

    // --

    s.writer.onNext(Form(30))

    assert(s.tryNow() == Success(Form(30)))
    assert(s.now() == Form(30))
    assert(s.signal.now() == Form(30))

    assert(d.tryNow() == Success(30))
    assert(d.now() == 30)
    assert(d.signal.now() == 30)

    // --

    s.update(f => f.copy(int = f.int + 1))

    assert(s.tryNow() == Success(Form(31)))
    assert(s.now() == Form(31))
    assert(s.signal.now() == Form(31))

    assert(d.tryNow() == Success(31))
    assert(d.now() == 31)
    assert(d.signal.now() == 31)

    // --

    s.tryUpdate(currTry => currTry.map(f => f.copy(int = f.int + 1)))

    assert(s.now() == Form(32))
    assert(s.signal.now() == Form(32))

    assert(d.now() == 32)
    assert(d.signal.now() == 32)
  }

  it("laziness, updates and errors") {

    val varOwner = new TestableOwner
    val obsOwner = new TestableOwner

    val s = Var(Form(10))
    val d = s.zoom(_.int)((f, int) => f.copy(int = int))(varOwner)

    val err1 = new Exception("Error: err1")

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(d.tryNow() == Success(10))
    assert(d.now() == 10)
    assert(d.signal.now() == 10)

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

    assert(s.tryNow() == Failure(err1))
    assert(d.tryNow() == Failure(err1))

    // Remember, a Var without a listener does emit its errors into "unhandled"
    errorEffects shouldBe mutable.Buffer(
      Effect("unhandled", VarError("Unable to update a failed Var. Consider Var#tryUpdate instead.", cause = Some(err1)))
    )
    errorEffects.clear()

    // -- Restore normality

    s.set(Form(0))

    assert(s.tryNow() == Success(Form(0)))
    assert(d.tryNow() == Success(0))

    // -- Can't update the same underlying var twice

    Var.set(
      s -> Form(1),
      d -> 2
    )
    errorEffects shouldBe mutable.Buffer(
      Effect("unhandled", VarError("Unable to Var.{set,setTry}: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.", cause = None))
    )
    errorEffects.clear()

    // -- Update again

    d.set(1)

    assert(s.tryNow() == Success(Form(1)))
    assert(s.now() == Form(1))
    assert(s.signal.now() == Form(1))

    assert(d.tryNow() == Success(1))
    assert(d.now() == 1)
    assert(d.signal.now() == 1)

    // --

    val effects = mutable.Buffer[Effect[Int]]()
    val obs = Observer[Int](effects += Effect("obs", _))

    d.signal.addObserver(obs)(obsOwner)

    varOwner.killSubscriptions() // we're specifically killing the owner that updates the derived var

    assert(effects.toList == List(Effect("obs", 1)))

    effects.clear()

    // --

    // Derived var is still active because of obsOwner, even though varOwner is dead already.
    // This is consistent with StrictSignal behaviour.

    d.set(2)

    assert(s.tryNow() == Success(Form(2)))
    assert(s.now() == Form(2))
    assert(s.signal.now() == Form(2))

    assert(d.tryNow() == Success(2))
    assert(d.now() == 2)
    assert(d.signal.now() == 2)

    assert(effects.toList == List(Effect("obs", 2)))

    effects.clear()

    // --

    obsOwner.killSubscriptions()

    assert(effects.isEmpty)

    // Now the derived var is killed

    // --

    d.set(34)

    // See comments in DerivedVar regarding error behaviour

    assert(s.now() == Form(2))
    assert(s.signal.now() == Form(2))

    assert(d.now() == 2)
    assert(d.signal.now() == 2)

    assert(errorEffects.toList == List(
      Effect("unhandled", VarError("Unable to set current value Success(34) on inactive derived var", None))
    ))

    errorEffects.clear()
  }


  it("signal does not glitch") {

    val owner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    val s = Var(Form(1))
    val d = s.zoom(_.int)((f, v) => f.copy(int = v))(owner)

    val combinedSignal = d.signal.combineWithFn(s.signal)(_ * 1000 + _.int) // e.g. if s.now() is Form(2), this is 2002

    val sourceObs = Observer[Form](f => effects += Effect("source-obs", f.int))
    val derivedObs = Observer[Int](effects += Effect("derived-obs", _))
    val combinedObs = Observer[Int](effects += Effect("combined-obs", _))

    assert(s.tryNow() == Success(Form(1)))
    assert(s.now() == Form(1))
    assert(s.signal.now() == Form(1))

    assert(d.tryNow() == Success(1))
    assert(d.now() == 1)
    assert(d.signal.now() == 1)

    assert(calculations == mutable.Buffer())

    // --

    s.set(Form(2))

    assert(s.tryNow() == Success(Form(2)))
    assert(s.now() == Form(2))
    assert(s.signal.now() == Form(2))

    assert(d.tryNow() == Success(2))
    assert(d.now() == 2)
    assert(d.signal.now() == 2)

    assert(calculations == mutable.Buffer())
    assert(effects == mutable.Buffer())

    // --

    // #Note observers are added to Var signals directly
    s.signal.addObserver(sourceObs)(owner)
    d.signal.addObserver(derivedObs)(owner)
    combinedSignal.addObserver(combinedObs)(owner)

    assert(effects.toList == List(
      Effect("source-obs", 2),
      Effect("derived-obs", 2),
      Effect("combined-obs", 2002),
    ))

    calculations.clear()
    effects.clear()

    // --

    // @TODO Why does the order of calculations (source vs derived) switch here?
    //  - Probably something to do with previous calculation being the first one after observers were added?
    //  - Our contract doesn't really mandate any order since these are derived MapSignal-s, not

    s.set(Form(3))

    assert(effects.toList == List(
      Effect("source-obs", 3),
      Effect("derived-obs", 3),
      Effect("combined-obs", 3003)
    ))

    calculations.clear()
    effects.clear()

    // --

    d.set(4)

    assert(effects.toList == List(
      Effect("source-obs", 4),
      Effect("derived-obs", 4),
      Effect("combined-obs", 4004)
    ))

    calculations.clear()
    effects.clear()

    // --

    val derivedAdder = d.updater[Int](_ + _)

    derivedAdder.onNext(10)

    assert(effects.toList == List(
      Effect("source-obs", 14),
      Effect("derived-obs", 14),
      Effect("combined-obs", 14014)
    ))
  }

  it("error handling") {

    val varOwner = new TestableOwner
    val obsOwner = new TestableOwner

    val s = Var(Form(10))
    val d = s.zoom(_.int)((f, int) => f.copy(int = int))(varOwner)

    val sub = d.signal.addObserver(Observer.empty)(obsOwner)

    val err1 = new Exception("Error: err1")
    val err2 = new Exception("Error: err2")

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(d.tryNow() == Success(10))
    assert(d.now() == 10)
    assert(d.signal.now() == 10)

    // -- Errors propagate

    s.setTry(Failure(err1))

    assert(s.tryNow() == Failure(err1))
    assert(d.tryNow() == Failure(err1))

    assert(errorEffects.toList == List(
      Effect("unhandled", err1),
      Effect("unhandled", err1) // two observers (one built into the derived var) – two errors
    ))

    errorEffects.clear()

    // -- Can't update a failed var

    d.update(_ + 1)

    assert(s.tryNow() == Failure(err1))
    assert(d.tryNow() == Failure(err1))

    // Remember, a Var without a listener does emit its errors into "unhandled"
    errorEffects shouldBe mutable.Buffer(
      Effect("unhandled", VarError("Unable to update a failed Var. Consider Var#tryUpdate instead.", cause = Some(err1)))
    )
    errorEffects.clear()

    // -- Reset

    s.set(Form(20))

    assert(s.tryNow() == Success(Form(20)))
    assert(d.tryNow() == Success(20))

    // -- Set error on the derived var

    d.setError(err1)

    assert(s.tryNow() == Failure(err1))
    assert(d.tryNow() == Failure(err1))

    assert(errorEffects.toList == List(
      Effect("unhandled", err1),
      Effect("unhandled", err1) // two observers (one built into the derived var) – two errors
    ))

    errorEffects.clear()

    // -- Update the error in the parent

    s.tryUpdate(_ => Failure(err2))

    assert(s.tryNow() == Failure(err2))
    assert(d.tryNow() == Failure(err2))

    assert(errorEffects.toList == List(
      Effect("unhandled", err2),
      Effect("unhandled", err2) // two observers (one built into the derived var) – two errors
    ))

    errorEffects.clear()

    // -- Update the error in the derived var

    // #TODO[API] Not sure what the desired behaviour here would be.
    //  Would we want to replace err1 with err2 in `s` and `d` vars? but we can't,
    //  because zooming out currently requires parent's value. Perhaps we should have
    //  a separate channel for zooming out errors? But I'm not sure what exactly the API
    //  should look like.

    d.tryUpdate(_ => Failure(err1))

    assert(s.tryNow() == Failure(err2))
    assert(d.tryNow() == Failure(err2))

    assert(errorEffects.toList == List(
      Effect("unhandled", VarError("Unable to zoom out of derived var when the parent var is failed.", Some(err2)))
    ))

    errorEffects.clear()
  }

  it("asymmetric derived vars") {

    val varOwner = new TestableOwner

    val s = Var(Form(10))
    val d1 = s.zoom(_.int)((f, int) => f.copy(int = 100 + int))(varOwner)
    val d2 = s.zoom(_.int + 100)((f, int) => f.copy(int = int))(varOwner)

    val err1 = new Exception("Error: err1")

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(d1.tryNow() == Success(10))
    assert(d1.now() == 10)
    assert(d1.signal.now() == 10)

    assert(d2.tryNow() == Success(110))
    assert(d2.now() == 110)
    assert(d2.signal.now() == 110)

    // -- Update parent var

    s.set(Form(20))

    assert(s.tryNow() == Success(Form(20)))
    assert(s.now() == Form(20))
    assert(s.signal.now() == Form(20))

    assert(d1.tryNow() == Success(20))
    assert(d1.now() == 20)
    assert(d1.signal.now() == 20)

    assert(d2.tryNow() == Success(120))
    assert(d2.now() == 120)
    assert(d2.signal.now() == 120)

    // -- Update derived var with misaligned zoomOut

    d1.set(30)

    assert(s.tryNow() == Success(Form(130)))
    assert(s.now() == Form(130))
    assert(s.signal.now() == Form(130))

    assert(d1.tryNow() == Success(130))
    assert(d1.now() == 130)
    assert(d1.signal.now() == 130)

    assert(d2.tryNow() == Success(230))
    assert(d2.now() == 230)
    assert(d2.signal.now() == 230)

    // -- Reset

    s.set(Form(20))

    assert(s.tryNow() == Success(Form(20)))
    assert(s.now() == Form(20))
    assert(s.signal.now() == Form(20))

    assert(d1.tryNow() == Success(20))
    assert(d1.now() == 20)
    assert(d1.signal.now() == 20)

    assert(d2.tryNow() == Success(120))
    assert(d2.now() == 120)
    assert(d2.signal.now() == 120)

    // -- Update derived var with misaligned zoomIn

    d2.set(40)

    assert(s.tryNow() == Success(Form(40)))
    assert(s.now() == Form(40))
    assert(s.signal.now() == Form(40))

    assert(d1.tryNow() == Success(40))
    assert(d1.now() == 40)
    assert(d1.signal.now() == 40)

    assert(d2.tryNow() == Success(140))
    assert(d2.now() == 140)
    assert(d2.signal.now() == 140)

  }
}
