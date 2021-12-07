package com.raquo.airstream.combine

import com.raquo.airstream.common.{InternalParentObserver, MultiParentEventStream, Observation}
import com.raquo.airstream.core.{EventStream, Observable, Protected, SyncObservable, Transaction, WritableEventStream}
import com.raquo.airstream.util.JsPriorityQueue

import scala.scalajs.js

/** Stream that emit events from all of its parents.
  *
  * Note: this stream re-emits errors emitted by all of its parents
  *
  * This feature exists only for EventStream-s because merging Signals
  * does not make sense, conceptually (what do you even do with their current values?).
  */
class MergeEventStream[A](
  override protected[this] val parents: Seq[EventStream[A]],
) extends WritableEventStream[A] with SyncObservable[A] with MultiParentEventStream[A, A] {

  override protected val topoRank: Int = Protected.maxTopoRank(parents) + 1

  private[this] val pendingParentValues: JsPriorityQueue[Observation[A]] = {
    new JsPriorityQueue(observation => Protected.topoRank(observation.observable))
  }

  private[this] val parentObservers: js.Array[InternalParentObserver[A]] = js.Array()

  parents.foreach(parent => parentObservers.push(makeInternalObserver(parent)))

  // @TODO document this, and document the topo parent order
  /** If this stream has already fired in a given transaction, the next firing will happen in a new transaction.
    *
    * This is needed for a combination of two reasons:
    * 1) only one event can propagate in a transaction at the same time
    * 2) We do not want the merged stream to "swallow" events
    *
    * We made it this way because the user probably expects this behavior.
    */
  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    // @TODO[Integrity] I don't think we actually need this "check":
    // At least one value is guaranteed to exist if this observable is pending
    // pendingParentValues.dequeue().value.fold(fireError(_, transaction), fireValue(_, transaction))

    while (pendingParentValues.nonEmpty) {
      pendingParentValues.dequeue().value.fold(
        fireError(_, transaction),
        fireValue(_, transaction)
      )
    }
  }

  override protected[this] def onStart(): Unit = {
    parentObservers.foreach(_.addToParent(shouldCallMaybeWillStart = false))
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    parentObservers.foreach(_.removeFromParent())
    super.onStop()
  }

  private def makeInternalObserver(parent: Observable[A]): InternalParentObserver[A] = {
    InternalParentObserver.fromTry(parent, (nextValue, transaction) => {
      pendingParentValues.enqueue(new Observation(parent, nextValue))
      // @TODO[API] Actually, why are we checking for .contains here? We need to better define behaviour
      // @TODO Make a test case that would exercise this .contains check or lack thereof
      // @TODO I think this check is moot because we can't have an observable emitting more than once in a transaction. Or can we? I feel like we can't/ It should probably be part of the transaction contract.
      if (!transaction.pendingObservables.contains(this)) {
        transaction.pendingObservables.enqueue(this)
      }
    })
  }
}
