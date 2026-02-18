package com.raquo.airstream.split_other

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.split.DuplicateKeysConfig
import com.raquo.airstream.state.{StrictSignal, Val, Var}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

// #Warning: this test is not in the `split` package to make sure that Scala 2.13 specific implicits
//  in the split package will be resolved correctly even outside of that package.

class SplitSomeSeqSpec extends UnitSpec with BeforeAndAfter {

  private val originalDuplicateKeysConfig = DuplicateKeysConfig.default

  case class Foo(id: String, version: Int)

  case class Bar(id: String)

  case class Element(id: String, fooSignal: StrictSignal[Foo]) {
    override def toString: String = s"Element($id, fooSignal)"
  }

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
    DuplicateKeysConfig.setDefault(originalDuplicateKeysConfig)

    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    assert(errorEffects.isEmpty) // #Note this fails the test rather inelegantly
  }

  it("splitSomeSeq signal") {

    val owner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()

    var elements = Seq.empty[Element]

    val v = Var(Option(Seq(Foo("a", 1))))

    val elementsS =
      v.signal
        .splitSomeSeq(_.id) { fooS =>
          effects += Effect("init", fooS.key)
          val signal = fooS.tapEach(foo => effects += Effect(s"signal-${fooS.key}", foo.toString))
          Element(fooS.key, signal)
        }
        .seqOrElse(
          ifEmptySeq = Element("empty-seq", Val(Foo("empty-seq", 1))),
          ifNone = Element("none", Val(Foo("none", 1)))
        )

    // --

    elementsS.foreach { els =>
      elements = els
    }(owner)

    assertEquals(
      effects.toList,
      List(Effect("init", "a"))
    )
    effects.clear()

    assertEquals(
      elements.map(el => (el.id, el.fooSignal.now())),
      List(
        ("a", Foo("a", 1))
      )
    )

    assertEquals(
      effects.toList,
      List(Effect("signal-a", "Foo(a,1)"))
    )
    effects.clear()

    // --

    v.set(
      Some(Seq(Foo("a", 2), Foo("b", 1)))
    )

    assertEquals(
      effects.toList,
      List(Effect("init", "b"))
    )
    effects.clear()

    assertEquals(
      elements.map(el => (el.id, el.fooSignal.now())),
      List(
        ("a", Foo("a", 2)),
        ("b", Foo("b", 1))
      )
    )

    assertEquals(
      effects.toList,
      List(
        Effect("signal-a", "Foo(a,2)"),
        Effect("signal-b", "Foo(b,1)")
      )
    )
    effects.clear()

    // --

    v.set(Some(Nil))

    assertEquals(elements.map(_.id), List("empty-seq"))
    assertEquals(effects.toList, Nil)

    // --

    v.set(None)

    assertEquals(elements.map(_.id), List("none"))
    assertEquals(effects.toList, Nil)
  }

  it("splitSomeSeqByIndex signal") {

    val owner = new TestableOwner

    val effects = mutable.Buffer[Effect[String]]()

    var elements = Seq.empty[Element]

    val v = Var(Option(Seq(Foo("a", 1))))

    val elementsS =
      v.signal
        .splitSomeSeqByIndex { fooS =>
          effects += Effect("init", fooS.key.toString)
          val signal = fooS.tapEach(foo => effects += Effect(s"signal-${fooS.key}", foo.toString))
          Element(fooS.key.toString, signal)
        }
        .seqOrElse(
          ifEmptySeq = Element("empty-seq", Val(Foo("empty-seq", 1))),
          ifNone = Element("none", Val(Foo("none", 1)))
        )

    // --

    elementsS.foreach { els =>
      elements = els
    }(owner)

    assertEquals(
      effects.toList,
      List(Effect("init", "0"))
    )
    effects.clear()

    assertEquals(
      elements.map(el => (el.id, el.fooSignal.now())),
      List(
        ("0", Foo("a", 1))
      )
    )

    assertEquals(
      effects.toList,
      List(Effect("signal-0", "Foo(a,1)"))
    )
    effects.clear()

    // --

    v.set(
      Some(Seq(Foo("aa", 2), Foo("b", 1)))
    )

    assertEquals(
      effects.toList,
      List(Effect("init", "1"))
    )
    effects.clear()

    assertEquals(
      elements.map(el => (el.id, el.fooSignal.now())),
      List(
        ("0", Foo("aa", 2)),
        ("1", Foo("b", 1))
      )
    )

    assertEquals(
      effects.toList,
      List(
        Effect("signal-0", "Foo(aa,2)"),
        Effect("signal-1", "Foo(b,1)")
      )
    )
    effects.clear()

    // --

    v.set(Some(Nil))

    assertEquals(elements.map(_.id), List("empty-seq"))
    assertEquals(effects.toList, Nil)

    // --

    v.set(None)

    assertEquals(elements.map(_.id), List("none"))
    assertEquals(effects.toList, Nil)
  }
}
