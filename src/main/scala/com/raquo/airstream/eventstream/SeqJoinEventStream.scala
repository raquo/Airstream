package com.raquo.airstream.eventstream

class SeqJoinEventStream[A](
  parents: Seq[EventStream[A]]
) extends CombineNEventStream[A, Seq[A]](parents) {

  override protected def toOut(seq: Seq[A]): Seq[A] = seq

}
