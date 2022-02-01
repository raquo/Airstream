package com.raquo.airstream.common

import com.raquo.airstream.core.{Observable, Protected, WritableStream}

/** A simple stream that has multiple parents. */
trait MultiParentStream[+I, O] extends WritableStream[O] {

  protected[this] val parents: Seq[Observable[I]]

  override protected def onWillStart(): Unit = {
    parents.foreach(Protected.maybeWillStart)
  }

}
