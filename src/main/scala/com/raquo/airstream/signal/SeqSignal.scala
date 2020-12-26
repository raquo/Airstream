package com.raquo.airstream.signal

class SeqSignal[A](
  parents: Seq[Signal[A]]
) extends CombineNSignal[A, Seq[A]](parents) {

  override protected def toOut(seq: Seq[A]): Seq[A] = seq

}
