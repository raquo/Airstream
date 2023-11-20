package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.fixtures.Effect
import com.raquo.airstream.ownership.{DynamicOwner, DynamicSubscription, Subscription}

import scala.collection.mutable

class SharedStartStreamSpec extends UnitSpec {

  it("EventStream.fromValue() / CustomStreamSource") {

    val dynOwner = new DynamicOwner(() => throw new Exception("Accessing dynamic owner after it is killed"))

    val stream = EventStream.fromValue(1)

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))
    val obs2 = Observer[Int](effects += Effect("obs2", _))

    // Wrapping in DynamicSubscription put us inside a Transaction.shared block.
    // This is (kind of) similar to how Laminar activates multiple subscriptions.
    DynamicSubscription.unsafe(
      dynOwner,
      activate = { o =>
        val sub1 = stream.addObserver(obs1)(o)
        val sub2 = stream.addObserver(obs2)(o)
        new Subscription(o, cleanup = () => {
          sub1.kill()
          sub2.kill()
        })
      }
    )

    assert(effects.isEmpty)

    // --

    dynOwner.activate()

    assert(effects.toList == List(
      Effect("obs1", 1),
      Effect("obs2", 1),
    ))
    effects.clear()

    // --

    dynOwner.deactivate()
    assert(effects.isEmpty)

    // --

    dynOwner.activate()

    assert(effects.toList == List(
      Effect("obs1", 1),
      Effect("obs2", 1),
    ))
    effects.clear()

  }
}
