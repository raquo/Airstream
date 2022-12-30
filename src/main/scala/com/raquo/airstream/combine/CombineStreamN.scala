package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentStream}
import com.raquo.airstream.core.{EventStream, Observable, Protected}
import com.raquo.ew.JsArray

import scala.scalajs.js
import scala.util.Try

/**
  * @param parentStreams Never update this array - this stream owns it.
  * @param combinator Must not throw! Must be pure.
  */
class CombineStreamN[A, Out](
  parentStreams: JsArray[EventStream[A]],
  combinator: JsArray[A] => Out
) extends MultiParentStream[A, Out] with CombineObservable[Out] {

  // @TODO[API] Maybe this should throw if parents.isEmpty

  override protected[this] val parents: JsArray[Observable[A]] = {
    // #Note this is safe as long as we don't put non-streams into this JsArray.
    parentStreams.asInstanceOf[JsArray[Observable[A]]]
  }

  override protected val topoRank: Int = Protected.maxTopoRank(parents) + 1

  private[this] val maybeLastParentValues: JsArray[js.UndefOr[Try[A]]] = parents.map(_ => js.undefined)

  override protected[this] def inputsReady: Boolean = {
    var allReady: Boolean = true
    maybeLastParentValues.forEach { lastValue =>
      if (lastValue.isEmpty) {
        allReady = false
      }
    }
    allReady
  }

  override protected[this] def combinedValue: Try[Out] = {
    // #Note don't call this unless you have first verified that
    //  inputs are ready, otherwise this asInstanceOf will not be safe.
    CombineObservable.jsArrayCombinator(maybeLastParentValues.asInstanceOf[JsArray[Try[A]]], combinator)
  }

  parents.forEachWithIndex { (parent, ix) =>
    parentObservers.push(
      InternalParentObserver.fromTry[A](
        parent,
        (nextParentValue, transaction) => {
          maybeLastParentValues.update(ix, nextParentValue)
          if (inputsReady) {
            onInputsReady(transaction)
          }
        }
      )
    )
  }

}
