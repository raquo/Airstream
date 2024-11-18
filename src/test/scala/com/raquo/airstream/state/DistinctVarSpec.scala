package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class DistinctVarSpec extends UnitSpec with BeforeAndAfter {

  private val effects = mutable.Buffer[Effect[Foo]]()

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

  it("distinct") {
    val owner = new TestableOwner
    val sourceVar = Var(Foo(1))

    val distinctVar = sourceVar.distinct

    // --

    sourceVar.signal.foreach { v =>
      effects += Effect("source", v)
    }(owner)

    distinctVar.signal.foreach { v =>
      effects += Effect("distinct", v)
    }(owner)

    assertEquals(
      effects.toList,
      List(
        Effect("source", Foo(1)),
        Effect("distinct", Foo(1))
      )
    )
    effects.clear()

    // --

    assertEquals(sourceVar.now(), Foo(1))
    assertEquals(distinctVar.now(), Foo(1))

    assertEquals(effects.toList, Nil)

    // --

    sourceVar.set(Foo(2))

    assertEquals(sourceVar.now(), Foo(2))
    assertEquals(distinctVar.now(), Foo(2))

    assertEquals(
      effects.toList,
      List(
        Effect("source", Foo(2)),
        Effect("distinct", Foo(2))
      )
    )
    effects.clear()

    // --

    sourceVar.set(Foo(2))

    assertEquals(sourceVar.now(), Foo(2))
    assertEquals(distinctVar.now(), Foo(2))

    assertEquals(
      effects.toList,
      List(
        Effect("source", Foo(2))
      )
    )
    effects.clear()

    // --

    sourceVar.set(Foo(2))

    assertEquals(sourceVar.now(), Foo(2))
    assertEquals(distinctVar.now(), Foo(2))

    assertEquals(
      effects.toList,
      List(
        Effect("source", Foo(2)),
      )
    )
    effects.clear()

    // --

    sourceVar.set(Foo(3))

    assertEquals(sourceVar.now(), Foo(3))
    assertEquals(distinctVar.now(), Foo(3))

    assertEquals(
      effects.toList,
      List(
        Effect("source", Foo(3)),
        Effect("distinct", Foo(3))
      )
    )
    effects.clear()

    // --

    distinctVar.set(Foo(3))

    assertEquals(sourceVar.now(), Foo(3))
    assertEquals(distinctVar.now(), Foo(3))

    assertEquals(effects.toList, Nil)

    // --

    sourceVar.set(Foo(3))

    assertEquals(sourceVar.now(), Foo(3))
    assertEquals(distinctVar.now(), Foo(3))

    assertEquals(
      effects.toList,
      List(
        Effect("source", Foo(3)),
      )
    )
    effects.clear()
  }
}
