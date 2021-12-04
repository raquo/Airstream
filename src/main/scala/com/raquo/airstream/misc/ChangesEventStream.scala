package com.raquo.airstream.misc

import com.raquo.airstream.common.{InternalTryObserver, SingleParentEventStream}
import com.raquo.airstream.core.{Protected, Signal, Transaction}

import scala.util.Try

class ChangesEventStream[A](
  override protected[this] val parent: Signal[A]
  //emitChangeOnRestart: Boolean
) extends SingleParentEventStream[A, A] with InternalTryObserver[A] {

  //private var maybeLastSeenValue: js.UndefOr[Try[A]] = js.undefined
  //
  //private var emitSignalValueOnNextStart: js.UndefOr[Try[A]] = js.undefined

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
    //maybeLastSeenValue = js.defined(nextValue)
    fireTry(nextValue, transaction)
  }

  //override protected def onWillStart(): Unit = {
  //  val maybePrevEmittedValue = maybeLastSeenValue
  //  super.onWillStart()
  //  if (emitChangeOnRestart) {
  //    maybePrevEmittedValue.foreach { prevValue =>
  //      val nextValue = parent.tryNow()
  //      // I guess what we really want to test here is "did parent emit while the stream was stopped?"
  //      if (nextValue != prevValue) { // #Note[Sync] We might need to let users bypass this filter eventually (or not, we'll see)
  //        emitSignalValueOnNextStart = nextValue
  //      }
  //    }
  //  }
  //}
  //
  //override protected[this] def onStart(): Unit = {
  //  super.onStart()
  //  emitSignalValueOnNextStart.foreach { nextValue =>
  //    maybeLastSeenValue = nextValue
  //    // #TODO[Integrity] I'm not sure if such cases should be a whole new transaction,
  //    //  or some kind of high priority post-current-transaction hook. This is weird
  //    //  because normally this stream emits in the same transaction as its parent
  //    println("changesStream scheduled trx")
  //    // #TODO[trx,sync] Creating a new transaction is not safe – causes glitches (duh)
  //    //  - I'm not sure if the `emitChangeOnRestart` option can be safely implemented or not.
  //    //  - I tried thinking about it, but couldn't confidently convince myself either way.
  //    //  - At first glance, in the simple case, an observable being started should be able to emit in the CURRENT transaction:
  //    //    since it's just being started, it hasn't emitted yet, so it won't emit more than once.
  //    //  - But that's not actually the case. The observable could have previously been started and stopped in this same transaction.
  //    //  - Or it may be possible for it to emit LATER, after some other ChangeStream (one of its ancestors) is started and emits a
  //    //    transaction. Not sure if that's actually possible.
  //    //  - Could we maybe get around some of this by tracking which transaction the observable was last stopped in, or by using
  //    //    the pending observables mechanism, or something like that? I'm not sure, and I don't have the time to prove it either way.
  //    new Transaction(fireTry(nextValue, _))
  //    emitSignalValueOnNextStart = js.undefined
  //  }
  //}
  //
  //override protected[this] def onStop(): Unit = {
  //  super.onStop()
  //}
}
