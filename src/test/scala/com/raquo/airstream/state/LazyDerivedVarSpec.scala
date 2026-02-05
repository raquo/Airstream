package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success}

class LazyDerivedVarSpec extends UnitSpec with BeforeAndAfter {

  case class Form(int: Int)

  case class MetaForm(form: Form)

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

  it("lazy eval") {

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val s = Var(Form(10))
    val d = s.zoom[Int](
      in = form => {
        effects += s"zoomIn-${form.int}"
        form.int
      }
    )(
      out = (form, int) => {
        val newForm = form.copy(int = int)
        effects += s"zoomOut-${newForm}"
        newForm
      }
    )

    assert(effects.toList == Nil)

    // --

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(10))
    assert(d.now() == 10)
    assert(d.signal.now() == 10)

    assert(effects.toList == List(
      "zoomIn-10"
    ))
    effects.clear()

    // --

    d.writer.onNext(20)

    assert(effects.toList == List(
      "zoomOut-Form(20)"
    ))
    effects.clear()

    assert(s.tryNow() == Success(Form(20)))
    assert(s.now() == Form(20))
    assert(s.signal.now() == Form(20))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(20))
    assert(d.now() == 20)
    assert(d.signal.now() == 20)

    // #TODO We evaluate zoomIn when accessing d.tryNow(),
    //  even though we set the value of `d` directly, because
    //  setting the value on `d` actually sets the value on its
    //  parent `s`, and we need to derive `d`'s value from the
    //  value of `s`... even when the value of `s` is coming from
    //  `d` in the first place.
    //  This "zoomIn-20" calculation is redundant and thus undesirable,
    //  but I don't see an elegant way to fix it. zoomPure is
    //  supposed to be pure, so it's not a big deal I think.
    assert(effects.toList == List(
      "zoomIn-20"
    ))
    effects.clear()

    // --

    d.update(_ + 1)

    assert(effects.toList == List(
      "zoomOut-Form(21)"
    ))
    effects.clear()

    assert(s.now() == Form(21))
    assert(s.signal.now() == Form(21))

    assert(effects.toList == Nil)

    assert(d.now() == 21)
    assert(d.signal.now() == 21)

    assert(effects.toList == List(
      "zoomIn-21" // #TODO this one is also undesirable, see TODO comment above.
    ))
    effects.clear()

    // --

    d.tryUpdate(currTry => currTry.map(_ + 1))

    assert(effects.toList == List(
      "zoomOut-Form(22)"
    ))
    effects.clear()

    assert(s.now() == Form(22))
    assert(s.signal.now() == Form(22))

    assert(effects.toList == Nil)

    assert(d.now() == 22)
    assert(d.signal.now() == 22)

    assert(effects.toList == List(
      "zoomIn-22" // #TODO this one is also undesirable, see TODO comment above.
    ))
    effects.clear()

    // --

    s.writer.onNext(Form(30))

    assert(s.tryNow() == Success(Form(30)))
    assert(s.now() == Form(30))
    assert(s.signal.now() == Form(30))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(30))
    assert(d.now() == 30)
    assert(d.signal.now() == 30)

    assert(effects.toList == List(
      "zoomIn-30"
    ))
    effects.clear()

    // --

    s.update(f => f.copy(int = f.int + 1))

    assert(s.tryNow() == Success(Form(31)))
    assert(s.now() == Form(31))
    assert(s.signal.now() == Form(31))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(31))
    assert(d.now() == 31)
    assert(d.signal.now() == 31)

    assert(effects.toList == List(
      "zoomIn-31"
    ))
    effects.clear()

    // --

    s.tryUpdate(currTry => currTry.map(f => f.copy(int = f.int + 1)))

    assert(s.now() == Form(32))
    assert(s.signal.now() == Form(32))

    assert(effects.toList == Nil)

    assert(d.now() == 32)
    assert(d.signal.now() == 32)

    assert(effects.toList == List(
      "zoomIn-32"
    ))
    effects.clear()
  }

  it("laziness, updates and errors") {

    // val varOwner = new TestableOwner
    val obsOwner = new TestableOwner

    // val s = Var(Form(10))
    // val d = s.zoomPure(_.int)((f, int) => f.copy(int = int))

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val s = Var(Form(10))
    val d = s.zoom[Int](
      in = form => {
        effects += s"zoomIn-${form.int}"
        form.int
      }
    )(
      out = (form, int) => {
        val newForm = form.copy(int = int)
        effects += s"zoomOut-${newForm}"
        newForm
      }
    )

    val err1 = new Exception("Error: err1")

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(10))
    assert(d.now() == 10)
    assert(d.signal.now() == 10)

    assert(effects.toList == List(
      "zoomIn-10"
    ))
    effects.clear()

    // -- Errors propagate

    s.setTry(Failure(err1))

    assert(s.tryNow() == Failure(err1))
    assert(d.tryNow() == Failure(err1))

    assert(effects.toList == Nil)

    assert(errorEffects.toList == List(
      Effect("unhandled", err1)
    ))

    errorEffects.clear()

    // -- Can't update a failed var

    d.update(_ + 1)

    assert(s.tryNow() == Failure(err1))
    assert(d.tryNow() == Failure(err1))

    assert(effects.toList == Nil)

    // Remember, a Var without a listener does emit its errors into "unhandled"
    errorEffects shouldBe mutable.Buffer(
      Effect("unhandled", VarError("Unable to update a failed Var. Consider Var#tryUpdate instead.", cause = Some(err1)))
    )
    errorEffects.clear()

    // -- Restore normality

    s.set(Form(0))

    assert(s.tryNow() == Success(Form(0)))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(0))

    assert(effects.toList == List(
      "zoomIn-0"
    ))
    effects.clear()

    // -- Can't update the same underlying var twice

    Var.set(
      s -> Form(1),
      d -> 2
    )
    assert(effects.toList == Nil)
    errorEffects shouldBe mutable.Buffer(
      Effect("unhandled", VarError("Unable to Var.{set,setTry}: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.", cause = None))
    )
    errorEffects.clear()

    // -- Update again

    d.set(1)

    assert(s.tryNow() == Success(Form(1)))
    assert(s.now() == Form(1))
    assert(s.signal.now() == Form(1))

    assert(effects.toList == List(
      "zoomOut-Form(1)"
    ))
    effects.clear()

    assert(d.tryNow() == Success(1))
    assert(d.now() == 1)
    assert(d.signal.now() == 1)

    assert(effects.toList == List(
      "zoomIn-1"
    ))
    effects.clear()

    // --

    val obs = Observer[Int](v => effects += s"obs-$v")

    d.signal.addObserver(obs)(obsOwner)

    assert(effects.toList == List("obs-1"))
    effects.clear()

    // --

    // Derived var is still active because of obsOwner.
    // This is consistent with StrictSignal behaviour.
    // DerivedVar behaves similarly if its owner is killed.

    d.set(2)

    assert(effects.toList == List(
      "zoomOut-Form(2)",
      "zoomIn-2",
      "obs-2"
    ))
    effects.clear()

    assert(s.tryNow() == Success(Form(2)))
    assert(s.now() == Form(2))
    assert(s.signal.now() == Form(2))

    assert(d.tryNow() == Success(2))
    assert(d.now() == 2)
    assert(d.signal.now() == 2)

    assert(effects.toList == Nil)

    // --

    obsOwner.killSubscriptions()

    assert(effects.isEmpty)

    // Now the derived var is killed

    // --

    d.set(34)

    // #TODO This is different from DerivedVar, which throws an error in this case – intentional?
    assert(effects.toList == List(
      "zoomOut-Form(34)"
    ))
    effects.clear()

    assert(s.now() == Form(34))
    assert(s.signal.now() == Form(34))

    assert(effects.isEmpty)

    assert(d.now() == 34)
    assert(d.signal.now() == 34)

    assert(effects.toList == List(
      "zoomIn-34"
    ))
    effects.clear()

    assert(effects.isEmpty)
    assert(errorEffects.isEmpty)
  }

  it("nested lazy + lazy") {

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val s = Var(MetaForm(Form(10)))

    val d1 = s.zoom(
      in = metaForm => {
        effects += s"d1.zoomIn-${metaForm.form}"
        metaForm.form
      }
    )(
      out = (metaForm, form) => {
        val newForm = metaForm.copy(form = form)
        effects += s"d1.zoomOut-${newForm}"
        newForm
      }
    )

    val d2 = d1.zoom(
      in = form => {
        effects += s"d2.zoomIn-${form.int}"
        form.int
      }
    )(
      out = (form, int) => {
        val newForm = form.copy(int = int)
        effects += s"d2.zoomOut-${newForm}"
        newForm
      }
    )

    assert(effects.toList == Nil)

    // --

    assert(s.tryNow() == Success(MetaForm(Form(10))))
    assert(s.now() == MetaForm(Form(10)))
    assert(s.signal.now() == MetaForm(Form(10)))

    assert(effects.toList == Nil)

    assert(d1.tryNow() == Success(Form(10)))
    assert(d1.now() == Form(10))
    assert(d1.signal.now() == Form(10))

    assert(effects.toList == List(
      "d1.zoomIn-Form(10)"
    ))
    effects.clear()

    assert(d2.tryNow() == Success(10))
    assert(d2.now() == 10)
    assert(d2.signal.now() == 10)

    assert(effects.toList == List(
      "d2.zoomIn-10"
    ))
    effects.clear()

    // --

    d2.set(20)

    assert(effects.toList == List(
      "d2.zoomOut-Form(20)",
      "d1.zoomOut-MetaForm(Form(20))"
    ))
    effects.clear()

    assert(s.tryNow() == Success(MetaForm(Form(20))))
    assert(s.now() == MetaForm(Form(20)))
    assert(s.signal.now() == MetaForm(Form(20)))

    assert(effects.toList == Nil)

    assert(d1.tryNow() == Success(Form(20)))
    assert(d1.now() == Form(20))
    assert(d1.signal.now() == Form(20))

    assert(effects.toList == List(
      "d1.zoomIn-Form(20)"
    ))
    effects.clear()

    assert(d2.tryNow() == Success(20))
    assert(d2.now() == 20)
    assert(d2.signal.now() == 20)

    assert(effects.toList == List(
      "d2.zoomIn-20"
    ))
    effects.clear()

    // --

    d1.set(Form(30))

    assert(effects.toList == List(
      "d1.zoomOut-MetaForm(Form(30))"
    ))
    effects.clear()

    assert(s.tryNow() == Success(MetaForm(Form(30))))
    assert(s.now() == MetaForm(Form(30)))
    assert(s.signal.now() == MetaForm(Form(30)))

    assert(effects.toList == Nil)

    assert(d1.tryNow() == Success(Form(30)))
    assert(d1.now() == Form(30))
    assert(d1.signal.now() == Form(30))

    assert(effects.toList == List(
      "d1.zoomIn-Form(30)"
    ))
    effects.clear()

    assert(d2.tryNow() == Success(30))
    assert(d2.now() == 30)
    assert(d2.signal.now() == 30)

    assert(effects.toList == List(
      "d2.zoomIn-30"
    ))
    effects.clear()
  }

  it("nested lazy + strict") {

    val d2Owner = new TestableOwner

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val s = Var(MetaForm(Form(10)))

    val d1 = s.zoom(
      in = metaForm => {
        effects += s"d1.zoomIn-${metaForm.form}"
        metaForm.form
      }
    )(
      out = (metaForm, form) => {
        val newForm = metaForm.copy(form = form)
        effects += s"d1.zoomOut-${newForm}"
        newForm
      }
    )

    val d2 = d1.zoomStrict(
      in = form => {
        effects += s"d2.zoomIn-${form.int}"
        form.int
      }
    )(
      out = (form, int) => {
        val newForm = form.copy(int = int)
        effects += s"d2.zoomOut-${newForm}"
        newForm
      }
    )(d2Owner)

    assert(effects.toList == List(
      "d1.zoomIn-Form(10)",
      "d2.zoomIn-10"
    ))
    effects.clear()

    // --

    assert(s.tryNow() == Success(MetaForm(Form(10))))
    assert(s.now() == MetaForm(Form(10)))
    assert(s.signal.now() == MetaForm(Form(10)))

    assert(d1.tryNow() == Success(Form(10)))
    assert(d1.now() == Form(10))
    assert(d1.signal.now() == Form(10))

    assert(d2.tryNow() == Success(10))
    assert(d2.now() == 10)
    assert(d2.signal.now() == 10)

    assert(effects.toList == Nil)

    // --

    d2.set(20)

    assert(effects.toList == List(
      "d2.zoomOut-Form(20)",
      "d1.zoomOut-MetaForm(Form(20))",
      "d1.zoomIn-Form(20)",
      "d2.zoomIn-20"
    ))
    effects.clear()

    assert(s.tryNow() == Success(MetaForm(Form(20))))
    assert(s.now() == MetaForm(Form(20)))
    assert(s.signal.now() == MetaForm(Form(20)))

    assert(d1.tryNow() == Success(Form(20)))
    assert(d1.now() == Form(20))
    assert(d1.signal.now() == Form(20))

    assert(d2.tryNow() == Success(20))
    assert(d2.now() == 20)
    assert(d2.signal.now() == 20)

    assert(effects.toList == Nil)

    // --

    d1.set(Form(30))

    assert(effects.toList == List(
      "d1.zoomOut-MetaForm(Form(30))",
      "d1.zoomIn-Form(30)",
      "d2.zoomIn-30"
    ))
    effects.clear()

    assert(s.tryNow() == Success(MetaForm(Form(30))))
    assert(s.now() == MetaForm(Form(30)))
    assert(s.signal.now() == MetaForm(Form(30)))

    assert(d1.tryNow() == Success(Form(30)))
    assert(d1.now() == Form(30))
    assert(d1.signal.now() == Form(30))

    assert(d2.tryNow() == Success(30))
    assert(d2.now() == 30)
    assert(d2.signal.now() == 30)

    assert(effects.toList == Nil)
  }

  it("nested strict + lazy") {

    val d1Owner = new TestableOwner

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val s = Var(MetaForm(Form(10)))

    val d1 = s.zoomStrict(
      in = metaForm => {
        effects += s"d1.zoomIn-${metaForm.form}"
        metaForm.form
      }
    )(
      out = (metaForm, form) => {
        val newForm = metaForm.copy(form = form)
        effects += s"d1.zoomOut-${newForm}"
        newForm
      }
    )(d1Owner)

    val d2 = d1.zoom(
      in = form => {
        effects += s"d2.zoomIn-${form.int}"
        form.int
      }
    )(
      out = (form, int) => {
        val newForm = form.copy(int = int)
        effects += s"d2.zoomOut-${newForm}"
        newForm
      }
    )

    assert(effects.toList == List(
      "d1.zoomIn-Form(10)"
    ))
    effects.clear()

    // --

    assert(s.tryNow() == Success(MetaForm(Form(10))))
    assert(s.now() == MetaForm(Form(10)))
    assert(s.signal.now() == MetaForm(Form(10)))

    assert(d1.tryNow() == Success(Form(10)))
    assert(d1.now() == Form(10))
    assert(d1.signal.now() == Form(10))

    assert(effects.toList == Nil)

    assert(d2.tryNow() == Success(10))
    assert(d2.now() == 10)
    assert(d2.signal.now() == 10)

    assert(effects.toList == List(
      "d2.zoomIn-10"
    ))
    effects.clear()

    // --

    d2.set(20)

    assert(effects.toList == List(
      "d2.zoomOut-Form(20)",
      "d1.zoomOut-MetaForm(Form(20))",
      "d1.zoomIn-Form(20)"
    ))
    effects.clear()

    assert(s.tryNow() == Success(MetaForm(Form(20))))
    assert(s.now() == MetaForm(Form(20)))
    assert(s.signal.now() == MetaForm(Form(20)))

    assert(d1.tryNow() == Success(Form(20)))
    assert(d1.now() == Form(20))
    assert(d1.signal.now() == Form(20))

    assert(effects.toList == Nil)

    assert(d2.tryNow() == Success(20))
    assert(d2.now() == 20)
    assert(d2.signal.now() == 20)

    assert(effects.toList == List(
      "d2.zoomIn-20"
    ))
    effects.clear()

    // --

    d1.set(Form(30))

    assert(effects.toList == List(
      "d1.zoomOut-MetaForm(Form(30))",
      "d1.zoomIn-Form(30)"
    ))
    effects.clear()

    assert(s.tryNow() == Success(MetaForm(Form(30))))
    assert(s.now() == MetaForm(Form(30)))
    assert(s.signal.now() == MetaForm(Form(30)))

    assert(d1.tryNow() == Success(Form(30)))
    assert(d1.now() == Form(30))
    assert(d1.signal.now() == Form(30))

    assert(effects.toList == Nil)

    assert(d2.tryNow() == Success(30))
    assert(d2.now() == 30)
    assert(d2.signal.now() == 30)

    assert(effects.toList == List(
      "d2.zoomIn-30"
    ))
    effects.clear()
  }

  it("signal does not glitch") {

    val owner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()

    val s = Var(Form(1))
    val d = s.zoom(_.int)((f, v) => f.copy(int = v))

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

    // --

    s.set(Form(2))

    assert(s.tryNow() == Success(Form(2)))
    assert(s.now() == Form(2))
    assert(s.signal.now() == Form(2))

    assert(d.tryNow() == Success(2))
    assert(d.now() == 2)
    assert(d.signal.now() == 2)

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

    effects.clear()

    // --

    d.set(4)

    assert(effects.toList == List(
      Effect("source-obs", 4),
      Effect("derived-obs", 4),
      Effect("combined-obs", 4004)
    ))

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

    val obsOwner = new TestableOwner

    val s = Var(Form(10))
    val d = s.zoom(_.int)((f, int) => f.copy(int = int))

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
    ))

    errorEffects.clear()

    // -- Update the error in the parent

    s.tryUpdate(_ => Failure(err2))

    assert(s.tryNow() == Failure(err2))
    assert(d.tryNow() == Failure(err2))

    assert(errorEffects.toList == List(
      Effect("unhandled", err2)
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
      Effect("unhandled", VarError("Unable to zoom out of lazy derived var when the parent var is failed.", Some(err2)))
    ))

    errorEffects.clear()
  }

  it("asymmetric derived vars") {

    val s = Var(Form(10))
    val d1 = s.zoom(_.int)((f, int) => f.copy(int = 100 + int))
    val d2 = s.zoom(_.int + 100)((f, int) => f.copy(int = int))

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
