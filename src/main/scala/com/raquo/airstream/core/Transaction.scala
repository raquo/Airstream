package com.raquo.airstream.core

import com.raquo.airstream.util.JsPriorityQueue
import com.raquo.ew.JsMap

// @TODO[Naming] Should probably be renamed to something like "Propagation"
/** @param code Note: Must not throw! */
class Transaction(private[Transaction] var code: Transaction => Any) {

  // @TODO this is not used except for debug logging. Remove eventually
  //val id: Int = Transaction.nextId()

  //println(s"  - create trx $id")

  /** Priority queue of pending observables: sorted by their topoRank.
    *
    * Corollary: An Observable that is dequeue-d from here does not synchronously depend on any other pending observables
    */
  private[airstream] val pendingObservables: JsPriorityQueue[SyncObservable[_]] = {
    new JsPriorityQueue(Protected.topoRank)
  }

  Transaction.pendingTransactions.add(this)

  @inline private[Transaction] def resolvePendingObservables(): Unit = {
    while (pendingObservables.nonEmpty) {
      //dom.console.log("RANKS: ", pendingObservables.debugQueue.map(_.topoRank))
      // Fire the first pending observable and remove it from the list
      pendingObservables.dequeue().syncFire(this)
    }
  }
}

object Transaction { // extends GlobalCounter {

  private object pendingTransactions {

    /** first transaction is the top of the stack, currently running */
    private var stack: List[Transaction] = Nil

    private val children: JsMap[Transaction, List[Transaction]] = new JsMap()

    def add(newTransaction: Transaction): Unit = {
      // 1. Regarding calling `run`:
      //    If a transaction is currently running, the new transaction will be triggered
      //    from the .done() call after the current transaction finishes.
      //    Otherwise, if there are no pending transactions other than this new transaction,
      //    we need to run this transaction right now because no one will do it for us.
      // 2. Regarding the queue:
      //    If a transaction is currently running, add newTransaction to its children.
      //    They will run after the current transaction finishes.
      peekStack().fold {
        pushToStack(newTransaction)
        run(newTransaction)
      }{ currentTransaction =>
        enqueueChild(parent = currentTransaction, newChild = newTransaction)
      }
    }

    def done(transaction: Transaction): Unit = {
      //println(s"--done trx: ${transaction.id}")
      //if (lastId > 50) {
      //  throw new Exception(">>> Overflow!!!!!")
      //}
      //println("current stack (LEFT is first): " + stack.map(_.id))
      //println("current children: " + pendingTransactions.children.map(t => (t._1.id, t._2.map(_.id))))
      if (!peekStack().contains(transaction)) {
        // @TODO[Integrity] Should we really throw here?
        throw new Exception("Transaction queue error: Completed transaction is not the first in stack. This is a bug in Airstream.")
      }

      putNextTransactionOnStack(doneTransaction = transaction)

      transaction.code = throwDeadTrxError  // stop holding up `trx` contents in memory

      peekStack().fold {
        if (children.size > 0) {
          //println(s"Stack is empty but children remain: ${children.map(t => (t._1.id, t._2.map(_.id)))}")
          var numChildren = 0
          children.forEach((transactions, _) => numChildren += transactions.size)
          throw new Exception(s"Transaction queue error: Stack cleared, but a total of ${numChildren} children for ${children.size} transactions remain. This is a bug in Airstream.")
        }
      }{ nextTransaction =>
        run(nextTransaction)
      }
    }

    /* If this transaction has children remaining, set first child to be run next.
     * Otherwise, remove transaction from the stack and do the same for next transaction on stack.
     */
    def putNextTransactionOnStack(doneTransaction: Transaction): Unit = {
      // We use depth-first because of https://github.com/raquo/Airstream/issues/39
      dequeueChild(parent = doneTransaction).fold[Unit] {
        // No children, this transaction is truly done now, remove it from the stack.
        popStack()
        // If any transactions left in the stack, recurse
        peekStack().foreach { parentTransaction =>
          putNextTransactionOnStack(doneTransaction = parentTransaction)
        }
      }{ nextChild =>
        // Found a child transaction, so put it on the stack, so that it wil run next.
        // Once that child is all done, it will be popped from the stack, and we will
        //
        //
        pushToStack(nextChild)
      }
    }

    def peekStack(): Option[Transaction] = {
      stack.headOption
    }

    private def childrenFor(transaction: Transaction): List[Transaction] = {
      children.get(transaction).getOrElse(Nil)
    }

    private def pushToStack(transaction: Transaction): Unit = {
      //println(s"pushToStack ${transaction.id}")
      stack = transaction :: stack
    }

    private def popStack(): Option[Transaction] = {
      //println(s"popStack")
      val result = stack.headOption
      if (result.nonEmpty) {
        //println("- was nonEmpty")
        stack = stack.tail
      }
      result
    }

    private def enqueueChild(parent: Transaction, newChild: Transaction): Unit = {
      //println(s"enqueueChild parent = ${parent.id} newChild = ${newChild.id}")
      val newChildren = childrenFor(parent) :+ newChild
      children.set(parent, newChildren)
    }

    private def dequeueChild(parent: Transaction): Option[Transaction] = {
      //println(s"dequeueChild parent = ${parent.id}")
      val parentChildren = childrenFor(parent)
      if (parentChildren.nonEmpty) {
        val nextChild = parentChildren.head
        //println(s"- found some children, first: ${nextChild.id}")
        val updatedChildren = parentChildren.tail
        if (updatedChildren.nonEmpty) {
          children.set(parent, updatedChildren)
          //println("- removed child, some remaining")
        } else {
          children.delete(parent)
          //println("- no children left for this parent, removed parent.")
        }
        Some(nextChild)
      } else {
        //println("- no children")
        None
      }
    }

    private[core] def isClearState: Boolean = stack.isEmpty && children.size == 0
  }

  private[core] def isClearState: Boolean = pendingTransactions.isClearState

  private[airstream] def currentTransaction(): Option[Transaction] = pendingTransactions.peekStack()

  private def run(transaction: Transaction): Unit = {
    //println(s"--start trx ${transaction.id}")
    try {
      transaction.code(transaction) // @TODO[API] Shouldn't we guard against exceptions in `code` here? It can be provided by the user.
      transaction.resolvePendingObservables()
    } finally {
      // @TODO[API,Integrity]
      //  This block is executed regardless of whether an exception is thrown in `code` or not,
      //  but it doesn't actually catch the exception, so `new Transaction(code)` actually throws
      //  iff `code` throws AND the transaction was created while no other transaction is running
      //  This is not very predictable, so we should fix it.
      //println(s"--end trx ${transaction.id}")
      pendingTransactions.done(transaction)
    }
  }

  private val throwDeadTrxError: Transaction => Any = { trx =>
    throw new Exception(s"Attempted to run Transaction $trx after it was already executed.")
  }

}
