package com.raquo.airstream.state

import com.raquo.airstream.core.Protected
import com.raquo.airstream.misc.MapSignal

import scala.util.Try

class LazyDerivedVarSignal[I, O](
  parent: Var[I],
  zoomIn: I => O,
  parentDisplayName: => String
) extends MapSignal[I, O](parent.signal, project = zoomIn, recover = None) with StrictSignal[O] { self =>

  // Note that even if owner kills subscription, this signal might remain due to other listeners
  // override protected[state] def isStarted: Boolean = super.isStarted

  override protected def defaultDisplayName: String = parentDisplayName + ".signal"

  override def tryNow(): Try[O] = {
    val newParentLastUpdateId = Protected.lastUpdateId(parent.signal)
    if (newParentLastUpdateId != _parentLastUpdateId) {
      // This branch can only run if !isStarted
      val nextValue = currentValueFromParent()
      updateCurrentValueFromParent(nextValue, newParentLastUpdateId)
      nextValue
    } else {
      super.tryNow()
    }
  }

  override protected[state] def updateCurrentValueFromParent(nextValue: Try[O], nextParentLastUpdateId: Int): Unit =
    super.updateCurrentValueFromParent(nextValue, nextParentLastUpdateId)
}
