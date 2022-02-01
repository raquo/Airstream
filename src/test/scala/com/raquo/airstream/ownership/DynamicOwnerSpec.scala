package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.Effect
import com.raquo.airstream.state.Var

import scala.collection.mutable

class DynamicOwnerSpec extends UnitSpec {

  it("Dynamic owner activation and deactivation") {

    val bus1 = new EventBus[Int]
    val var2 = Var(0)

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))
    val obs2 = Observer[Int](effects += Effect("obs2", _))

    val dynOwner = new DynamicOwner(() => fail("Attempted to use permakilled owner!"))

    DynamicSubscription.unsafe(dynOwner, owner => bus1.events.addObserver(obs1)(owner))

    bus1.writer.onNext(100)

    effects shouldBe mutable.Buffer()
    dynOwner.isActive shouldBe false
    dynOwner.maybeCurrentOwner shouldBe None

    // --

    dynOwner.activate()

    effects shouldBe mutable.Buffer()
    dynOwner.isActive shouldBe true

    // --

    bus1.writer.onNext(200)
    effects shouldBe mutable.Buffer(Effect("obs1", 200))
    effects.clear()

    // --

    val dynSub2 = DynamicSubscription.subscribeObserver(dynOwner, var2.signal, obs2)
    effects shouldBe mutable.Buffer(Effect("obs2", 0))
    effects.clear()

    // --

    dynOwner.deactivate()
    bus1.writer.onNext(300)
    var2.writer.onNext(3)

    effects shouldBe mutable.Buffer()

    // --

    dynOwner.activate() // this subscribes to the signal. It remembers 3 despite deactivation because Var is a StrictSignal. Not the best test I guess.
    bus1.writer.onNext(400)
    var2.writer.onNext(4)

    effects shouldBe mutable.Buffer(Effect("obs2", 3), Effect("obs1", 400), Effect("obs2", 4))
    effects.clear()

    // --

    bus1.writer.onNext(500)
    var2.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs1", 500), Effect("obs2", 5))
    effects.clear()

    // --

    dynSub2.kill() // permanently deactivated and removed from owner
    bus1.writer.onNext(600)
    var2.writer.onNext(6)

    effects shouldBe mutable.Buffer(Effect("obs1", 600))
    effects.clear()

    // --

    dynOwner.deactivate()
    bus1.writer.onNext(700)
    var2.writer.onNext(7)

    effects shouldBe mutable.Buffer()

    // --

    dynOwner.activate()

    effects shouldBe mutable.Buffer()

    // --

    bus1.writer.onNext(800)
    var2.writer.onNext(8)

    effects shouldBe mutable.Buffer(Effect("obs1", 800))
    effects.clear()
  }
}
