package com.somebody.`else`

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.{Protected, Signal, Transaction}
import com.somebody.`else`.ExtensionSpec.ExtSignal

import scala.util.Try

class ExtensionSpec extends UnitSpec {

  it("Allow extension of observables including overriding and accessing all required methods") {
    val parent = Signal.fromValue(0)
    new ExtSignal[Int, String](parent, _.toString)
  }
}

object ExtensionSpec {

  class ExtSignal[I, O](
    override protected[this] val parent: Signal[I],
    project: I => O
  ) extends SingleParentSignal[I, O] with InternalTryObserver[I] {

    override protected val topoRank: Int = Protected.topoRank(parent) + 1

    override protected def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
      fireTry(nextParentValue.map(project), transaction)
    }

    override protected def currentValueFromParent(): Try[O] = Protected.tryNow(parent).map(project)
  }
}
