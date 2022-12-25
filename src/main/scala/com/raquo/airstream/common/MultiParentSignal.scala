package com.raquo.airstream.common

import com.raquo.airstream.core.{Observable, Protected, WritableSignal}

import scala.util.Try

/** A simple signal that has multiple parents. */
trait MultiParentSignal[+I, O] extends WritableSignal[O] {

  // #nc[perf] should this maybe be js.Array?
  protected[this] val parents: Seq[Observable[I]]

  override protected def onWillStart(): Unit = {
    parents.foreach(Protected.maybeWillStart)
    updateCurrentValueFromParent()
  }

  protected def updateCurrentValueFromParent(): Try[O] = {
    val nextValue = currentValueFromParent()
    setCurrentValue(nextValue)
    nextValue
  }

}
