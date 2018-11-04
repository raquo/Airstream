package com.raquo.airstream.core

import com.raquo.airstream.util.JsPriorityQueue

import scala.scalajs.js

// @TODO[Naming] Should probably be renamed to something like "Propagation"
class Transaction(private[Transaction] val code: Transaction => Any) {

  // @TODO this is not used except for debug logging. Remove eventually
  // val id: Int = Transaction.nextId()

  /** Priority queue of pending observables: sorted by their topoRank.
    *
    * Corollary: An Observable that is dequeue-d from here does not synchronously depend on any other pending observables
    */
  private[airstream] val pendingObservables: JsPriorityQueue[SyncObservable[_]] = new JsPriorityQueue(_.topoRank)

  Transaction.add(this)

  @inline private[Transaction] def resolvePendingObservables(): Unit = {
    while (pendingObservables.nonEmpty) {
      //      dom.console.log("RANKS: ", pendingObservables.map(_.topoRank))
      // Fire the first pending observable and remove it from the list
      pendingObservables.dequeue().syncFire(this)
    }
  }
}

object Transaction { // extends GlobalCounter {

  private var isSafeToRemoveObserver: Boolean = true

  private[this] val pendingTransactions: js.Array[Transaction] = js.Array()

  private[this] val pendingObserverRemovals: js.Array[() => Unit] = js.Array()

  /** Safely remove external observer (such that it doesn't interfere with iteration over the list of observers).
    * Removal still happens synchronously, just at the end of a transaction if one is running right now.
    */
  def removeExternalObserver[A](observable: Observable[A], observer: Observer[A]): Unit = {
    if (isSafeToRemoveObserver) {
      // remove right now – useful for efficient recursive removals
      observable.removeExternalObserverNow(observer)
    } else {
      // schedule removal to happen at the end of the transaction
      // (don't want to interfere with iteration over the list of observers)
      pendingObserverRemovals.push(() => observable.removeExternalObserverNow(observer))
    }
  }

  /** Safely remove internal observer (such that it doesn't interfere with iteration over the list of observers).
    * Removal still happens synchronously, just at the end of a transaction if one is running right now.
    */
  def removeInternalObserver[A](observable: Observable[A], observer: InternalObserver[A]): Unit = {
    if (isSafeToRemoveObserver) {
      // remove right now – useful for efficient recursive removals
      observable.removeInternalObserverNow(observer)
    } else {
      // schedule removal to happen at the end of the transaction
      // (don't want to interfere with iteration over observables' lists of observers)
      pendingObserverRemovals.push(() => observable.removeInternalObserverNow(observer))
    }
  }

  private def resolvePendingObserverRemovals(): Unit = {
    if (!isSafeToRemoveObserver) {
      throw new Exception("It's not safe to remove observers right now!")
    }
    pendingObserverRemovals.foreach(remove => remove())
    pendingObserverRemovals.clear()
  }

  private def add(transaction: Transaction): Unit = {
    // If a transaction is currently running, the new transaction will be triggered
    // from the .done() call after the current transaction finishes.
    // Otherwise, if there are no pending transactions other than this new transaction,
    // we need to run this transaction right now because no one will do it for us.
    val hasPendingTransactions = pendingTransactions.length > 0
    pendingTransactions.push(transaction)
    if (!hasPendingTransactions) {
      run(transaction)
    }
  }

  private def run(transaction: Transaction): Unit = {
    isSafeToRemoveObserver = false
    transaction.code(transaction)
    transaction.resolvePendingObservables()
    isSafeToRemoveObserver = true
    done(transaction)
  }

  private def done(transaction: Transaction): Unit = {
    if (pendingTransactions.head != transaction) {
      // @TODO[Integrity] Should we really throw here?
      throw new Exception("Transaction mismatch: done transaction is not first in list")
    }
    pendingTransactions.shift()

    resolvePendingObserverRemovals()

    if (pendingTransactions.length > 0) {
      run(pendingTransactions.head)
    }
  }

}
