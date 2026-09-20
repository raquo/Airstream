package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.common.SingleParentStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner
import org.scalactic.anyvals.NonEmptyList

import java.util.concurrent.Flow
import scala.collection.mutable
import scala.util.Try

class EventStreamSpec extends UnitSpec {

  it("EventStream.fromSeq emit on restart") {

    implicit val owner: Owner = new TestableOwner

    val range = 1 to 3
    val stream = EventStream.fromSeq(range)

    val effects = mutable.Buffer[Effect[?]]()
    val sub1 = stream.foreach(newValue => effects += Effect("obs1", newValue))

    effects.toList shouldBe range.map(i => Effect("obs1", i))
    effects.clear()

    sub1.kill()

    val sub2 = stream.foreach(newValue => effects += Effect("obs2", newValue))

    effects.toList shouldBe range.map(i => Effect("obs2", i))
    effects.clear()
  }

  it("EventStream.fromSeq.startWith emit on restart") {

    implicit val owner: Owner = new TestableOwner

    val range = 1 to 3
    val signal = EventStream.fromSeq(range).startWith(0)

    val effects = mutable.Buffer[Effect[?]]()
    val sub1 = signal.foreach(newValue => effects += Effect("obs1", newValue))

    effects.toList shouldBe (0 +: range).map(i => Effect("obs1", i))
    effects.clear()

    sub1.kill()

    val sub2 = signal.foreach(newValue => effects += Effect("obs2", newValue))

    effects.toList shouldBe (3 +: range).map(i => Effect("obs2", i))
    effects.clear()
  }

  it("filter") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val range = 0 to 10
    val stream = EventStream.fromSeq(range, emitOnce = true)

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = stream.filter(f).foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.filter(f).map(i => Effect("obs0", i))
  }

  it("filter reports throwing internal observers as unhandled errors") {
    implicit val owner: Owner = new TestableOwner
    val bus = new EventBus[Int]
    val stream = bus.events.filter(_ => true)
    val error = new Exception("internal observer failed")
    val effects = mutable.Buffer[Effect[?]]()
    val unhandledErrors = mutable.Buffer[Throwable]()
    val errorCallback: Throwable => Unit = err => { unhandledErrors += err; () }
    val throwing: SingleParentStream[Int, Int] = new SingleParentStream[Int, Int] {
      override protected val parent: EventStream[Int] = stream
      override protected val topoRank: Int = Protected.topoRank(parent) + 1
      override protected def onNext(nextValue: Int, transaction: Transaction): Unit = {
        throw error
      }
      override protected def onError(nextError: Throwable, transaction: Transaction): Unit = ()
      override protected def onTry(nextValue: Try[Int], transaction: Transaction): Unit = {
        nextValue.fold(onError(_, transaction), onNext(_, transaction))
      }
    }

    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    stream.foreach(value => effects += Effect("value", value))
    val throwingSubscription = throwing.foreach(_ => ())
    try {
      bus.emit(1)

      effects.toList shouldBe List(Effect("value", 1))
      unhandledErrors.toList shouldBe List(error)
    } finally {
      throwingSubscription.kill()
      AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
      AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    }
  }

  it("filterNot") {

    implicit val owner: Owner = new TestableOwner

    val f = (_: Int) % 2 == 0
    val range = 0 to 10
    val stream = EventStream.fromSeq(range, emitOnce = true)

    val effects = mutable.Buffer[Effect[?]]()
    val subscription0 = stream.filterNot(f).foreach(newValue => effects += Effect("obs0", newValue))

    subscription0.kill()
    effects.toList shouldBe range.filterNot(f).map(i => Effect("obs0", i))
  }

  it("collect") {

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[Either[String, Int]]

    val effects = mutable.Buffer[Effect[?]]()
    bus
      .events
      .collect { case Right(i) => i }
      .foreach(v => effects += Effect("obs", v))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Right(1))

    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )
    effects.clear()

    // --

    bus.emit(Right(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )
    effects.clear()

    // --

    bus.emit(Left("yo"))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(Right(3))

    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )
    effects.clear()

  }

  it("collectOpt") {

    // def NonEmptyList[A](list: List[A]): Option[List[A]] = {
    //  if (list.nonEmpty) Some(list) else None
    // }

    implicit val owner: Owner = new TestableOwner

    val bus = new EventBus[List[Int]]

    val effects = mutable.Buffer[Effect[?]]()
    bus
      .events
      .collectOpt(NonEmptyList.from(_))
      .foreach(v => effects += Effect("obs", v.head))

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(List(1))

    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )
    effects.clear()

    // --

    bus.emit(List(2))

    effects shouldBe mutable.Buffer(
      Effect("obs", 2)
    )
    effects.clear()

    // --

    bus.emit(Nil)

    effects shouldBe mutable.Buffer()

    // --

    bus.emit(List(3))

    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )
    effects.clear()

  }

}
