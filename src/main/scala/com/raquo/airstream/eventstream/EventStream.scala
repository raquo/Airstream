package com.raquo.airstream.eventstream

import com.raquo.airstream.core.{LazyObservable, MemoryObservable}
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.signal.{FoldSignal, Signal, SignalFromEventStream}
import com.raquo.airstream.state.{MapState, State}

import scala.concurrent.Future

trait EventStream[+A] extends LazyObservable[A] {

  override type Self[+T] = EventStream[T]

  override def map[B](project: A => B): EventStream[B] = {
    new MapEventStream(this, project)
  }

  def mapTo[B](value: B): EventStream[B] = {
    new MapEventStream[A, B](this, _ => value)
  }

  def filter(passes: A => Boolean): EventStream[A] = {
    new FilterEventStream(this, passes)
  }

  def collect[B](pf: PartialFunction[A, B]): EventStream[B] = {
    filter(pf.isDefinedAt).map(pf)
  }

  def delay(intervalMillis: Int = 0): EventStream[A] = {
    new DelayEventStream(parent = this, intervalMillis)
  }

  def throttle(intervalMillis: Int): EventStream[A] = {
    ThrottleEventStream(parent = this, intervalMillis)
  }

  def debounce(delayFromLastEventMillis: Int): EventStream[A] = {
    new DebounceEventStream(parent = this, delayFromLastEventMillis)
  }

  def fold[B](initial: B)(fn: (B, A) => B): Signal[B] = {
    new FoldSignal(parent = this, () => initial, fn)
  }

  def toSignal[B >: A](initial: B): Signal[B] = {
    new SignalFromEventStream(parent = this, initial)
  }

  def toWeakSignal: Signal[Option[A]] = {
    new SignalFromEventStream(parent = this.map(Some(_)), initialValue = None)
  }

  def toState[B >: A](initial: B)(implicit owner: Owner): State[B] = {
    new MapState[B, B](parent = this.toSignal(initial), project = identity, owner)
  }

  def compose[B](operator: EventStream[A] => EventStream[B]): EventStream[B] = {
    operator(this)
  }

  def combineWith[AA >: A, B](otherEventStream: EventStream[B]): CombineEventStream2[AA, B, (AA, B)] = {
    new CombineEventStream2(
      parent1 = this,
      parent2 = otherEventStream,
      combinator = (_, _)
    )
  }

  def withCurrentValueOf[B](memoryObservable: MemoryObservable[B]): EventStream[(A, B)] = {
    new SampleCombineEventStream2[A, B, (A, B)](
      samplingStream = this,
      sampledMemoryObservable = memoryObservable,
      combinator = (_, _)
    )
  }

  def sample[B](memoryObservable: MemoryObservable[B]): EventStream[B] = {
    new SampleCombineEventStream2[A, B, B](
      samplingStream = this,
      sampledMemoryObservable = memoryObservable,
      combinator = (_, sampledValue) => sampledValue
    )
  }
}

object EventStream {

  def fromSeq[A](events: Seq[A]): EventStream[A] = {
    new SeqEventStream[A](events)
  }

  def fromFuture[A](future: Future[A]): EventStream[A] = {
    new FutureEventStream(future, emitIfFutureCompleted = false)
  }

  @inline def combine[A, B](
    stream1: EventStream[A],
    stream2: EventStream[B]
  ): EventStream[(A, B)] = {
    stream1.combineWith(stream2)
  }

  def merge[A](streams: EventStream[A]*): EventStream[A] = {
    new MergeEventStream[A](streams)
  }

  implicit def toTuple2Stream[A, B](stream: EventStream[(A, B)]): Tuple2EventStream[A, B] = {
    new Tuple2EventStream(stream)
  }
}
