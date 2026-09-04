package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentSignal}
import com.raquo.airstream.core.{Protected, Signal}
import com.raquo.ew.JsArray

import scala.util.Try

/**
  * @param parents Never update this array - this signal owns it.
  * @param combinator Must not throw! Must be pure.
  */
class CombineSignalN[A, Out](
  override protected val parents: JsArray[Signal[A]],
  protected val combinator: JsArray[A] => Out
) extends MultiParentSignal[A, Out] with CombineObservable[Out] {

  // @TODO[API] Maybe this should throw if parents.isEmpty

  override protected val topoRank: Int = Protected.maxTopoRank(0, parents) + 1

  override protected val parentObservers: JsArray[InternalParentObserver[?]] = {
    parents.mapWithIndex { (parent, ix) =>
      InternalParentObserver.fromTry[A](parent, (_, trx) => {
        _parentLastUpdateIds.update(ix, Protected.lastUpdateId(parent)) // #TODO[Test] Note 100% sure that we need to be doing this. No test catches the difference right now.
        onInputsReady(trx)
      })
    }
  }

  override protected def inputsReady: Boolean = true

  override protected def combinedValue: Try[Out] = {
    CombineObservable.jsArrayCombinator(parents.map(_.tryNow()), combinator)
  }

  override protected def currentValueFromParent(): Try[Out] = combinedValue
}
