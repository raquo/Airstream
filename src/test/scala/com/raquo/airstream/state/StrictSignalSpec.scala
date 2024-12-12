package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError
import com.raquo.airstream.fixtures.Effect
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success}

class StrictSignalSpec extends UnitSpec with BeforeAndAfter {

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

  it("LazyStrictSignal: lazy eval") {

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val s = Var(Form(10))
    val d = s.signal.mapLazy[Int] { form =>
      effects += s"project-${form.int}"
      form.int
    }

    assert(effects.toList == Nil)

    // --

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(10))
    assert(d.now() == 10)

    assert(effects.toList == List(
      "project-10"
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

    assert(effects.toList == List(
      "project-30"
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

    assert(effects.toList == List(
      "project-31"
    ))
    effects.clear()

    // --

    s.tryUpdate(currTry => currTry.map(f => f.copy(int = f.int + 1)))

    assert(s.now() == Form(32))
    assert(s.signal.now() == Form(32))

    assert(effects.toList == Nil)

    assert(d.now() == 32)

    assert(effects.toList == List(
      "project-32"
    ))
    effects.clear()
  }

  it("LazyStrictSignal: laziness and errors") {

    val effects: mutable.Buffer[String] = mutable.Buffer()

    val s = Var(Form(10))
    val d = s.signal.mapLazy[Int] { form =>
      effects += s"project-${form.int}"
      form.int
    }

    val err1 = new Exception("Error: err1")

    assert(s.tryNow() == Success(Form(10)))
    assert(s.now() == Form(10))
    assert(s.signal.now() == Form(10))

    assert(effects.toList == Nil)

    assert(d.tryNow() == Success(10))
    assert(d.now() == 10)

    assert(effects.toList == List(
      "project-10"
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
  }
}
