package com.somebody.`else`

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.{BaseObservable, Observable, Signal, WritableSignal}

import scala.util.Try

class ExtensionSpec extends UnitSpec {

  it("Allow extension of observables including overriding and accessing all required methods") {

    class ExtSignal[I, O](parent: Signal[I], project: I => O) extends WritableSignal[O] {

      override protected val topoRank: Int = BaseObservable.topoRank(parent) + 1

      override protected def initialValue: Try[O] = ??? //parent.tryNow().map(project) // @nc
    }
  }
}
