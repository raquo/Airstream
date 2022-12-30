package com.raquo.airstream.common

import com.raquo.airstream.core.{Protected, Signal, WritableSignal}
import com.raquo.ew.JsArray

/** A simple signal that has multiple parents. */
trait MultiParentSignal[I, O] extends WritableSignal[O] {

  /** This array is read-only, never update it. */
  protected[this] val parents: JsArray[Signal[I]]

  protected[this] lazy val _parentLastUpdateIds: JsArray[Int] = parents.map(Protected.lastUpdateId(_))

  override protected def onWillStart(): Unit = {
    parents.forEach(Protected.maybeWillStart(_))
    val shouldPullFromParent = updateParentLastUpdateIds()
    if (shouldPullFromParent) {
      updateCurrentValueFromParent()
    }
  }

  /** @return Whether parent has emitted since last time we checked */
  protected[this] def updateParentLastUpdateIds(): Boolean = {
    var parentHasUpdated = false
    parents.forEachWithIndex { (parent, ix) => {
      val newLastUpdateId = Protected.lastUpdateId(parent)
      val lastSeenParentUpdateId = _parentLastUpdateIds(ix)
      if (newLastUpdateId != lastSeenParentUpdateId) {
        _parentLastUpdateIds.update(ix, newLastUpdateId)
        parentHasUpdated = true
      }
    }}
    parentHasUpdated
  }

  protected def updateCurrentValueFromParent(): Unit = {
    val nextValue = currentValueFromParent()
    setCurrentValue(nextValue)
  }

}
