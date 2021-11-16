package com.raquo.airstream.split

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{Protected, Transaction, WritableSignal}
import com.raquo.airstream.timing.SyncDelayEventStream

import scala.util.{Success, Try}

/** This signal MUST be sync delayed after the split signal to prevent it
  * from emitting unwanted events. The reason is... complicated.
  *
  * Also, this signal is initialized at a strange time. It depends on `parent`, but it is initialized while `parent` is
  * propagating an event. So this signal can be *started* during this propagation too (e.g. in Laminar use case). Since
  * this is a signal, when started it provides its initial value to its observers. However because the parent's event is
  * still propagating, it would ALSO cause this signal to fire that event – which is the same as its initial value. This
  * duplicity is undesirable of course, so we take care of it by dropping the first event after initialization IF
  * it happens during the transaction in which this signal was initialized.
  */
private[airstream] class SplitChildSignal[M[_], A](
  override protected[this] val parent: SyncDelayEventStream[M[A]],
  initial: A,
  getMemoizedValue: () => Option[A]
) extends WritableSignal[A] with SingleParentObservable[M[A], A] with InternalTryObserver[M[A]] {

  private var maybeInitialTransaction: Option[Transaction] = Transaction.currentTransaction()

  private var droppedDuplicateEvent: Boolean = false

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def initialValue: Try[A] = Success(initial)

  override protected def onTry(nextValue: Try[M[A]], transaction: Transaction): Unit = {
    getMemoizedValue().foreach { freshMemoizedInput =>
      // #Note I do think we want to compare both `None` and `Some` cases.
      //  I'm not sure if None is possible, but if it is, this is probably the right thing to do.
      //  I think None might be possible when evaluating this signal's initial value
      if (!droppedDuplicateEvent && maybeInitialTransaction == Transaction.currentTransaction()) {
        //println(s">>>>> DROPPED EVENT ${freshMemoizedInput}, TRX IS ${maybeInitialTransaction}")
        maybeInitialTransaction = None
        droppedDuplicateEvent = true
      } else {
        fireTry(Success(freshMemoizedInput), transaction)
      }
    }
  }

}
