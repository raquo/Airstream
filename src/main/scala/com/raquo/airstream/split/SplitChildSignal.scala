package com.raquo.airstream.split

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.{Protected, Transaction}
import com.raquo.airstream.timing.SyncDelayStream

import scala.scalajs.js
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
  *
  * @param getMemoizedValue get the latest memoized value and its corresponding parentLastUpdateId.
  */
private[airstream] class SplitChildSignal[M[_], A](
  override protected[this] val parent: SyncDelayStream[M[A]],
  initial: A,
  initialParentLastUpdateId: Int,
  getMemoizedValue: () => Option[(A, Int)]
) extends SingleParentSignal[M[A], A] with InternalTryObserver[M[A]] {

  /** Note: initial value is not an "event" and is not "emitted",
    * so its propagation when starting the signal does not count here.
    */
  private var hasEmittedEvents = false

  private var maybeInitialTransaction: js.UndefOr[Transaction] = Transaction.currentTransaction()

  private var droppedDuplicateEvent: Boolean = false

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  // onWillStart & currentValueFromParent:
  // If this child signal is started immediately after it's first initialized,
  // for example if we add an observer to it in the split operator's render callback,
  // then while it's being started, its initial value was not memoized yet,
  // so we pull it via the special channel (`initial` and `initialLastParentUpdateId`)

  override protected def onWillStart(): Unit = {
    // Sync to parent signal. This is similar to standard SingleParentSignal logic,
    // except `val parent` is a special timing stream, not the real parent signal,
    // so we need to ge the parent's value and lastUpdateId in a special manner.
    // dom.console.log(s"${this} > onWillStart")
    Protected.maybeWillStart(parent)
    // dom.console.log(s"  getMemoizedValue() = ${getMemoizedValue()} ")
    val newParentLastUpdateId = getMemoizedValue().map(_._2).getOrElse(initialParentLastUpdateId)
    if (newParentLastUpdateId != _parentLastUpdateId) {
      // Note: We only update the value and the parent update id on re-start if
      // the parent has updated while this signal was stopped.
      // Note that there is no deduplication at this stage. The typical distinctCompose
      // filtering is applied LATER, on top of this child signal's output.
      updateCurrentValueFromParent()
      _parentLastUpdateId = newParentLastUpdateId
    }
  }

  override protected def currentValueFromParent(): Try[A] = {
    // dom.console.log(s"$this -> currentValueFromParent")
    // #TODO[Sync] is this right, or is this right only in the context of Laminar usage of split?
    // #Note See also SignalFromStream for similar logic
    // #Note This can be called from inside tryNow(), so make sure to avoid an infinite loop
    if (maybeLastSeenCurrentValue.nonEmpty && hasEmittedEvents) {
      val m = getMemoizedValue()
      // dom.console.log(s"  = $m (memoized)")
      // #Note memoized value should always be available at this point. It's only unavailable
      //  under very specific conditions (see comment above), and we don't call this method
      //  in those conditions.
      Success(getMemoizedValue().get._1)
    } else {
      // dom.console.log(s"  = $initial (initial)")
      Success(initial)
    }
  }

  override protected def onTry(nextParentValue: Try[M[A]], transaction: Transaction): Unit = {
    getMemoizedValue().foreach { case (freshMemoizedInput, lastParentUpdateId) =>
      _parentLastUpdateId = lastParentUpdateId
      // #Note I do think we want to compare both `None` and `Some` cases of maybeTransaction.
      //  I'm not sure if None is possible, but if it is, this is probably the right thing to do.
      //  I think None might be possible when evaluating this signal's initial value when starting it
      if (!droppedDuplicateEvent && maybeInitialTransaction == Transaction.currentTransaction()) {
        //println(s">>>>> DROPPED EVENT ${freshMemoizedInput}, TRX IS ${maybeInitialTransaction}")
        maybeInitialTransaction = js.undefined
        droppedDuplicateEvent = true
      } else {
        hasEmittedEvents = true
        fireTry(Success(freshMemoizedInput), transaction)
      }
    }
  }

}
