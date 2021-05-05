package com.somebody.`else`

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{Protected, Signal, Transaction, WritableSignal}

import scala.util.Try

class ExtensionSpec extends UnitSpec {

  it("Allow extension of observables including overriding and accessing all required methods") {

    class ExtSignal[I, O](
      override protected[this] val parent: Signal[I],
      project: I => O
    ) extends WritableSignal[O] with SingleParentObservable[I, O] with InternalTryObserver[I] {

      override protected val topoRank: Int = Protected.topoRank(parent) + 1

      override protected def initialValue: Try[O] = Protected.tryNow(parent).map(project)

      override protected def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
        fireTry(nextParentValue.map(project), transaction)
      }
    }
  }
}
