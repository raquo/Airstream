package com.raquo.airstream.state

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{AirstreamError, Observer}
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success}

class LazyStrictSignalSpec extends UnitSpec with BeforeAndAfter {

  private val err1 = new Exception("err1")

  private val err2 = new Exception("err2")

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

  it("LazyStrictSignal.apply") {
    // We're not using this method directly in Airstream itself anywhere, so just a small test to ensure the basics.
    // This test previously would have failed because I forgot to add onTry implementation to this signal.
    // (it had a different non-mapSignal implementation back then)

    val owner = new TestableOwner
    val sourceVar = Var(Foo(1))

    val distinctSignal = LazyStrictSignal[Foo](
      sourceVar.signal.distinct, sourceVar.signal.displayName, displayNameSuffix = ".distinct*.signal"
    )

    // --

    sourceVar.signal.foreach { v =>
      effects += Effect("source", v)
    }(owner)

    distinctSignal.foreach { v =>
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
    assertEquals(distinctSignal.now(), Foo(1))

    assertEquals(effects.toList, Nil)

    // --

    sourceVar.set(Foo(2))

    assertEquals(sourceVar.now(), Foo(2))
    assertEquals(distinctSignal.now(), Foo(2))

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
    assertEquals(distinctSignal.now(), Foo(2))

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
    assertEquals(distinctSignal.now(), Foo(2))

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
    assertEquals(distinctSignal.now(), Foo(3))

    assertEquals(
      effects.toList,
      List(
        Effect("source", Foo(3)),
        Effect("distinct", Foo(3))
      )
    )
    effects.clear()

  }

  it("LazyStrictSignal can pull from parent LazyStrictSignal") {

    val v = Var(1)

    val s1 = v.signal.map(n => new Foo(n * 100)).map(Effect.log("s1", effects))
    val s2 = s1.map(f => f.copy(id = f.id + 1)).map(Effect.log("s2", effects))

    assertEquals(effects.toList, Nil)

    // -- call s2.tryNow() to pull the value through s1

    assertEquals(s2.tryNow(), Success(Foo(101)))
    assertEquals(
      effects.toList,
      List(
        Effect("s1", new Foo(100)),
        Effect("s2", new Foo(101))
      )
    )
    effects.clear()

    assertEquals(s1.tryNow(), Success(Foo(100)))
    assertEquals(effects.toList, Nil)

    // -- call s1.tryNow() to pull that one value first

    v.set(2)

    assertEquals(s1.tryNow(), Success(Foo(200)))
    assertEquals(
      effects.toList,
      List(
        Effect("s1", new Foo(200)),
      )
    )
    effects.clear()

    // -- call s2.tryNow() to pull only that value (s1 already pulled)

    assertEquals(s2.tryNow(), Success(Foo(201)))
    assertEquals(
      effects.toList,
      List(
        Effect("s2", new Foo(201)),
      )
    )
    effects.clear()

    // --

    v.setTry(Failure(err1))

    assertEquals(s2.tryNow(), Failure(err1))
    assertEquals(s1.tryNow(), Failure(err1))

  }

  it("LazyStrictSignal.distinct") {

    val v = Var(1)

    val s1 = v.signal.map(n => new Foo(n)).map(Effect.log("s1", effects))
    val s2 = s1.distinct.map(Effect.log("s2", effects))

    assertEquals(effects.toList, Nil)

    // --

    assertEquals(s2.tryNow(), Success(Foo(1)))
    assertEquals(effects.toList, List(Effect("s1", Foo(1)), Effect("s2", Foo(1))))
    effects.clear()

    assertEquals(s1.tryNow(), Success(Foo(1)))
    assertEquals(effects.toList, Nil)

    // --

    v.set(1)

    assertEquals(effects.toList, Nil)

    // --

    assertEquals(s2.tryNow(), Success(Foo(1)))
    assertEquals(effects.toList, List(Effect("s1", Foo(1))))
    effects.clear()

    assertEquals(s1.tryNow(), Success(Foo(1)))
    assertEquals(effects.toList, Nil)

    // --

    v.set(2)

    assertEquals(effects.toList, Nil)

    // --

    assertEquals(s2.tryNow(), Success(Foo(2)))
    assertEquals(effects.toList, List(Effect("s1", Foo(2)), Effect("s2", Foo(2))))
    effects.clear()

    assertEquals(s1.tryNow(), Success(Foo(2)))
    assertEquals(effects.toList, Nil)

    // --

    v.set(2)

    assertEquals(effects.toList, Nil)

    // --

    assertEquals(s2.tryNow(), Success(Foo(2)))
    assertEquals(effects.toList, List(Effect("s1", Foo(2))))
    effects.clear()

    assertEquals(s1.tryNow(), Success(Foo(2)))
    assertEquals(effects.toList, Nil)
  }

  it("LazyStrictSignal.debug") {

    val v = Var(1)
    val intEffects = mutable.Buffer[Effect[Int]]()

    val s1 = v.signal.map(_ * 100)
      .debugSpyEvents(Effect.log("s1-fire", intEffects))
      .debugSpyEvalFromParent(t => intEffects += Effect("s1-eval", t.getOrElse(-1)))
    val s2 = s1.map(_ + 1)
      .debugSpyEvents(Effect.log("s2-fire", intEffects))
      .debugSpyEvalFromParent(t => intEffects += Effect("s2-eval", t.getOrElse(-1)))

    assertEquals(intEffects.toList, Nil)

    // -- call s2.tryNow() to pull the value through s1

    assertEquals(s2.tryNow(), Success(101))
    assertEquals(
      intEffects.toList,
      List(
        Effect("s1-eval", 100),
        Effect("s2-eval", 101)
      )
    )
    intEffects.clear()

    assertEquals(s1.tryNow(), Success(100))
    assertEquals(intEffects.toList, Nil)

    // -- call s1.tryNow() to pull that one value first

    v.set(2)

    assertEquals(s1.tryNow(), Success(200))
    assertEquals(
      intEffects.toList,
      List(
        Effect("s1-eval", 200),
      )
    )
    intEffects.clear()

    // -- call s2.tryNow() to pull only that value (s1 already pulled)

    assertEquals(s2.tryNow(), Success(201))
    assertEquals(
      intEffects.toList,
      List(
        Effect("s2-eval", 201),
      )
    )
    intEffects.clear()

    // --

    v.setTry(Failure(err1))

    assertEquals(s2.tryNow(), Failure(err1))
    assertEquals(s1.tryNow(), Failure(err1))

    assertEquals(
      intEffects.toList,
      List(
        Effect("s1-eval", -1),
        Effect("s2-eval", -1),
      )
    )
    intEffects.clear()

    // --

    val owner = new TestableOwner

    s2.addObserver(Observer.fromTry(t => intEffects += Effect("obs", t.getOrElse(-1))
    ))(owner)

    assertEquals(
      intEffects.toList,
      List(
        Effect("obs", -1)
      )
    )
    intEffects.clear()

    // --

    v.set(3)

    assertEquals(
      intEffects.toList,
      List(
        Effect("s1-fire", 300),
        Effect("s2-fire", 301),
        Effect("obs", 301)
      )
    )
    intEffects.clear()
  }

  it("LazyStrictSignal.mapRecover") {

    val v = Var(1)
    val intEffects = mutable.Buffer[Effect[Int]]()

    val s1 =
      v.signal
        .map(_ * 100)
        .recover {
          case `err1` => Some(900)
          case `err2` => None
        }
        .map(Effect.log("s1", intEffects))
    val s2 =
      s1
        .map(_ + 1)
        .map(Effect.log("s2", intEffects))

    assertEquals(intEffects.toList, Nil)

    // -- call s2.tryNow() to pull the value through s1

    assertEquals(s2.tryNow(), Success(101))
    assertEquals(
      intEffects.toList,
      List(
        Effect("s1", 100),
        Effect("s2", 101)
      )
    )
    intEffects.clear()

    assertEquals(s1.tryNow(), Success(100))
    assertEquals(intEffects.toList, Nil)

    // -- call s1.tryNow() to pull that one value first

    v.set(2)

    assertEquals(s1.tryNow(), Success(200))
    assertEquals(
      intEffects.toList,
      List(
        Effect("s1", 200),
      )
    )
    intEffects.clear()

    // -- call s2.tryNow() to pull only that value (s1 already pulled)

    assertEquals(s2.tryNow(), Success(201))
    assertEquals(
      intEffects.toList,
      List(
        Effect("s2", 201),
      )
    )
    intEffects.clear()

    // --

    v.setTry(Failure(err1))

    assertEquals(s2.tryNow(), Success(901))
    assertEquals(s1.tryNow(), Success(900))

    assertEquals(
      intEffects.toList,
      List(
        Effect("s1", 900),
        Effect("s2", 901),
      )
    )
    intEffects.clear()

    // --

    v.set(3)

    assertEquals(s2.tryNow(), Success(301))
    assertEquals(
      intEffects.toList,
      List(
        Effect("s1", 300),
        Effect("s2", 301),
      )
    )
    intEffects.clear()

    // --

    // v.setTry(Failure(err2))
    //
    // assertEquals(s2.tryNow(), Success(301))
    // assertEquals(s1.tryNow(), Success(300))
    //
    // // #TODO[Integrity,API] We actually expect intEffects to be empty here.
    // //  But, even though we've "filtered out" / "swallowed" the `err2` error, the signal's value is
    // //  still considered to have updated, so we're re-calculating these values.
    // //  This seems problematic, and it's probably because we have a filter-like operator in Signals.
    // //  Signals probably need a facility to avoid incrementing their lastUpdateId if the filter
    // //  predicate fails.
    // assertEquals(
    //   intEffects.toList,
    //   List(
    //     Effect("s1", 300),
    //     Effect("s2", 301),
    //   )
    // )
    // intEffects.clear()
  }
}
