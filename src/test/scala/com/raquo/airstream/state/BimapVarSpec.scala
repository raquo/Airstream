package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError
import com.raquo.airstream.fixtures.Effect
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success}

class BimapVarSpec extends UnitSpec with BeforeAndAfter {

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

  case class Foo(id: Int)

  val err1 = new Exception("err1")
  val err2 = new Exception("err2")

  val err_x2 = new Exception("err_x2")
  val err_x5 = new Exception("err_x5")

  it("bimap") {
    val fooVar = Var(Foo(0))
    val intVar = fooVar.bimap(_.id)(Foo(_))

    assertEquals(fooVar.now(), Foo(0))
    assertEquals(intVar.now(), 0)

    // --

    fooVar.set(Foo(1))

    assertEquals(fooVar.now(), Foo(1))
    assertEquals(intVar.now(), 1)

    // --

    intVar.update(_ + 1)

    assertEquals(fooVar.now(), Foo(2))
    assertEquals(intVar.now(), 2)
  }

  it("bimap-errors") {
    // Updates to derived var are routed through the parent var,
    // so your getThis / getParent functions should not throw.
    // If they do throw, this is how it will behave.

    val fooVar = Var(Foo(1))
    val intVar = fooVar.bimap(
      getThis = f => if (f.id % 2 == 0) throw err_x2 else f.id
    )(
      getParent = i => if (i % 5 == 0) throw err_x5 else Foo(i)
    )

    assertEquals(fooVar.tryNow(), Success(Foo(1)))
    assertEquals(intVar.tryNow(), Success(1))

    // --

    // #TODO[API] err_x2 is not sent to unhandled errors, even though
    //  we may expect it to. This is because it's usually done in
    //  `fireTry`, whereas we're updating the derived signal using
    //  the pull (`updateCurrentValueFromParent`) mechanism, so
    //  `fireTry` is never called. Not yet sure if it's possible to
    //  improve on this. Same issue would apply to other lazy derived
    //  vars as well.

    fooVar.set(Foo(2))

    assertEquals(fooVar.tryNow(), Success(Foo(2)))
    assertEquals(intVar.tryNow(), Failure(err_x2))

    // --

    intVar.set(1)

    assertEquals(fooVar.tryNow(), Success(Foo(1)))
    assertEquals(intVar.tryNow(), Success(1))

    // --

    intVar.set(2)

    assertEquals(fooVar.tryNow(), Success(Foo(2)))
    assertEquals(intVar.tryNow(), Failure(err_x2))

    // --

    fooVar.set(Foo(3))

    assertEquals(fooVar.tryNow(), Success(Foo(3)))
    assertEquals(intVar.tryNow(), Success(3))

    // --

    intVar.update(_ + 2)

    assertEquals(fooVar.tryNow(), Failure(err_x5))
    assertEquals(intVar.tryNow(), Failure(err_x5))

    assertEquals(
      errorEffects.toList,
      List(
        Effect("unhandled", err_x5)
      )
    )
    errorEffects.clear()

    // --

    fooVar.set(Foo(5))

    assertEquals(fooVar.tryNow(), Success(Foo(5)))
    assertEquals(intVar.tryNow(), Success(5))

    // --

    intVar.setTry(Failure(err1))

    assertEquals(fooVar.tryNow(), Failure(err1))
    assertEquals(intVar.tryNow(), Failure(err1))

    assertEquals(
      errorEffects.toList,
      List(
        Effect("unhandled", err1)
      )
    )
    errorEffects.clear()

    // --

    fooVar.setTry(Failure(err2))

    assertEquals(fooVar.tryNow(), Failure(err2))
    assertEquals(intVar.tryNow(), Failure(err2))

    assertEquals(
      errorEffects.toList,
      List(
        Effect("unhandled", err2)
      )
    )
    errorEffects.clear()

  }
}
