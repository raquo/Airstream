package com.raquo.airstream.javaflow

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.fixtures.{Effect, TestableOwner}
import com.raquo.airstream.ownership.Owner

import java.util.concurrent.Flow
import scala.collection.mutable

class FlowPublisherStreamSpec extends UnitSpec {

  class RangePublisher(range: Range) extends Flow.Publisher[Int] {
    def subscribe(subscriber: Flow.Subscriber[_ >: Int]): Unit = {
      val subscription = new Flow.Subscription {
        def request(n: Long): Unit = range.foreach(subscriber.onNext(_))
        def cancel(): Unit = ()
      }
      subscriber.onSubscribe(subscription)
    }
  }

  it("EventStream.fromPublisher") {

    implicit val owner: Owner = new TestableOwner

    val range = 1 to 3
    val stream = EventStream.fromPublisher(new RangePublisher(range))

    val effects = mutable.Buffer[Effect[_]]()
    val sub1 = stream.foreach(newValue => effects += Effect("obs1", newValue))

    effects.toList shouldBe range.map(i => Effect("obs1", i))
    effects.clear()

    sub1.kill()

    val sub2 = stream.foreach(newValue => effects += Effect("obs2", newValue))

    effects.toList shouldBe range.map(i => Effect("obs2", i))
    effects.clear()
  }
}
