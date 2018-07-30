package com.raquo.airstream.eventbus

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner

class WriteBus[A] extends Observer[A] {

  private[eventbus] val stream: EventBusStream[A] = new EventBusStream(this)

  /** Note: this source will be removed when the `owner` you provide says so.
    * To remove this source manually, call .removeSource() on the resulting WriteBusSource.
    */
  def addSource(sourceStream: EventStream[A])(implicit owner: Owner): EventBusSource[A] = {
    new EventBusSource(stream, sourceStream, owner)
  }

  /** Behaves similar to `contramap`, but gives you a WriteBus, not just an Observer */
  def contramapWriter[B](project: B => A)(implicit owner: Owner): WriteBus[B] = {
    val mapBus = new WriteBus[B]
    addSource(mapBus.stream.map(project))(owner)
    mapBus
  }

  /** Behaves similar to `filter`, but gives you a WriteBus, not just an Observer */
  def filterWriter(passes: A => Boolean)(implicit owner: Owner): WriteBus[A] = {
    val filterBus = new WriteBus[A]
    addSource(filterBus.stream.filter(passes))(owner)
    filterBus
  }

  override def onNext(nextValue: A): Unit = {
    if (stream.isStarted) { // important check
      // @TODO[Integrity] We rely on the knowledge that EventBusStream discards the transaction it's given. Laaaame
      stream.onNext(nextValue, transaction = null)
    }
    // else {
    //   println(">>>> WriteBus.onNext called, but stream is not started!")
    // }
  }

  override def contramap[B](project: B => A): Observer[B] = {
    Observer(nextValue => onNext(project(nextValue)))
  }

  override def filter[B <: A](passes: B => Boolean): Observer[B] = {
    Observer(nextValue => if (passes(nextValue)) onNext(nextValue))
  }
}
