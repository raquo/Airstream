package com.raquo.airstream.common

import com.raquo.airstream.core.{Protected, Signal, WritableSignal}
import com.raquo.ew.JsArray

/** A simple signal that has multiple parents. */
trait MultiParentSignal[+I, O] extends WritableSignal[O] {

  // #nc[perf] should this maybe be js.Array?
  protected[this] val parents: Seq[Signal[I]]

  protected[this] lazy val _parentLastUpdateIds: JsArray[Int] = JsArray(parents.map(Protected.lastUpdateId): _*) // #nc inefficient

  override protected def onWillStart(): Unit = {
    // #nc redundant loop eh? ugly af
    parents.foreach(Protected.maybeWillStart)
    val shouldPullFromParent = updateParentLastTrxIds()
    if (shouldPullFromParent) {
      updateCurrentValueFromParent()
    }
  }

  /** @return Whether parent has emitted since last time we checked */
  protected[this] def updateParentLastTrxIds(): Boolean = {
    var ix = 0
    var parentHasUpdated = false
    while (ix < parents.size) {
      val newLastTrxId = Protected.lastUpdateId(parents(ix))
      val lastSeenParentTrxId = _parentLastUpdateIds(ix)
      if (newLastTrxId != lastSeenParentTrxId) {
        _parentLastUpdateIds.update(ix, newLastTrxId)
        parentHasUpdated = true
      }
      ix += 1
    }
    parentHasUpdated
  }

  protected def updateCurrentValueFromParent(): Unit = {
    val nextValue = currentValueFromParent()
    setCurrentValue(nextValue)
  }

}
