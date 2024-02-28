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
  * @param initialValue note that this can get stale - always try to read memoized value first.
  *
  * @param getMemoizedValue get the latest memoized value and its corresponding parentLastUpdateId.
  */
private[airstream] class SplitChildSignal[M[_], A](
  override protected[this] val parent: SyncDelayStream[M[A]],
  private[this] var initialValue: Option[(A, Int)],
  getMemoizedValue: () => Option[(A, Int)]
) extends SingleParentSignal[M[A], A] with InternalTryObserver[M[A]] {

  private var maybeInitialTransaction: js.UndefOr[Transaction] = Transaction.currentTransaction()

  private var droppedDuplicateEvent: Boolean = false

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onWillStart(): Unit = {
    // dom.console.log(s"${this} onWillStart")

    Protected.maybeWillStart(parent)
    val maybeInitialValue = pullValueFromParent()
    // dom.console.log(s"maybeInitialValue = ${maybeInitialValue}")

    maybeInitialValue.foreach {
      case (nextValue, nextParentLastUpdateId) =>
        // Sync to parent signal. This is similar to standard SingleParentSignal logic,
        // except `val parent` is a special timing stream, not the real parent signal,
        // so we need to ge the parent's value and lastUpdateId in a special manner.
        if (nextParentLastUpdateId != _parentLastUpdateId) {
          // Note: We only update the value and the parent update id on re-start if
          // the parent has updated while this signal was stopped.
          // Note that there is no deduplication at this stage. The typical distinctCompose
          // filtering is applied LATER, on top of this child signal's output.
          updateCurrentValueFromParent(Success(nextValue), nextParentLastUpdateId)
        }
    }
  }

  override protected def updateCurrentValueFromParent(
    nextValue: Try[A],
    nextParentLastUpdateId: Int
  ): Unit = {
    super.updateCurrentValueFromParent(nextValue, nextParentLastUpdateId)
    initialValue = None // No longer needed regardless of what `nextValue` is
  }

  override protected def currentValueFromParent(): Try[A] = {
    pullValueFromParent()
      .map(t => Success(t._1))
      .getOrElse(tryNow()) // #Note: Be careful to avoid infinite loop here
  }

  private def pullValueFromParent(): Option[(A, Int)] = {
    // - We try to look at the memoized value first, because
    //   it's possible that initialValue is outdated (e.g. if
    //   parent emitted after we created this child signal,
    //   but before we started this child signal
    // - But the memoized value is not always available, for
    //   example, it's not available if the child signal is
    //   started inside SplitSignal's render (`project`)
    //   callback, which is typical for Laminar use case.
    //   - The `initialValue` actually exists for those cases.
    getMemoizedValue().orElse(initialValue)
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
        // hasEmittedEvents = true
        fireTry(Success(freshMemoizedInput), transaction)
      }
    }
  }

}
