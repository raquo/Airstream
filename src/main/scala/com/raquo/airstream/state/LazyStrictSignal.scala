package com.raquo.airstream.state

import com.raquo.airstream.core.{Protected, Signal}
import com.raquo.airstream.misc.MapSignal

import scala.util.Try

/** #TODO[Naming,Org] Messy
  *
  * This signal offers the API of a [[StrictSignal]] but is actually
  * lazy. All it does is let you PULL the signal's current value.
  * This mostly works fine if your signal does not depend on any streams.
  *
  * We use this signal internally for derived Var use cases where we know
  * that it should work fine. we may change the naming and structure of
  * this class when we implement settle on a long term strategy for
  * peekNow / pullNow https://github.com/raquo/Laminar/issues/130
  */
class LazyStrictSignal[I, O](
  parentSignal: Signal[I],
  zoomIn: I => O,
  parentDisplayName: => String,
  displayNameSuffix: String
) extends MapSignal[I, O](parentSignal, project = zoomIn, recover = None) with StrictSignal[O] { self =>

  override protected def defaultDisplayName: String = parentDisplayName + displayNameSuffix + s"@${hashCode()}"

  override def tryNow(): Try[O] = {
    val newParentLastUpdateId = Protected.lastUpdateId(parentSignal)
    // #TODO This comparison only works when parentSignal is started or strict.
    //  - e.g. it does not help us in `split`, it only helps us with lazyZoom.
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
