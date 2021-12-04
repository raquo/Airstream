package com.raquo.airstream.common

import com.raquo.airstream.core.{Observable, Protected, WritableEventStream}

/** A simple stream that has multiple parents. */
trait MultiParentEventStream[+I, O] extends WritableEventStream[O] {

  protected[this] val parents: Seq[Observable[I]]

  override protected def onWillStart(): Unit = {
    parents.foreach(Protected.maybeWillStart)
  }

}
