package com.raquo.airstream.core

import com.raquo.airstream.util.JsPriorityQueue
import com.raquo.ew.{JsArray, JsMap}

import scala.scalajs.js

// @TODO[Naming] Should probably be renamed to something like "Propagation"
/** @param code Note: Must not throw! */
class Transaction(private[Transaction] var code: Transaction => Any) {

  // val id: Int = Transaction.nextId()

  //println(s"  - create trx $id")

  /** Priority queue of pending observables: sorted by their topoRank.
    *
    * Corollary: An Observable that is dequeue-d from here does not synchronously depend on any other pending observables
    */
  private[this] var maybePendingObservables: js.UndefOr[JsPriorityQueue[SyncObservable[_]]] = js.undefined

  Transaction.pendingTransactions.add(this)

  @inline private[Transaction] def resolvePendingObservables(): Unit = {
    maybePendingObservables.foreach { pendingObservables =>
      while (pendingObservables.nonEmpty) {
        //dom.console.log("RANKS: ", pendingObservables.debugQueue.map(_.topoRank))
        // Fire the first pending observable and remove it from the list
        pendingObservables.dequeue().syncFire(this)
      }
    }
  }

  private[airstream] def containsPendingObservable(observable: SyncObservable[_]): Boolean = {
    maybePendingObservables.map(_.contains(observable)).getOrElse(false)
  }

  private[airstream] def enqueuePendingObservable(observable: SyncObservable[_]): Unit = {
    val queue = maybePendingObservables.getOrElse {
      val newQueue = new JsPriorityQueue[SyncObservable[_]](Protected.topoRank)
      maybePendingObservables = newQueue
      newQueue
    }
    queue.enqueue(observable)
  }

}

object Transaction {

  /** This object holds a queue of callbacks that should be executed
    * when all observables finish starting. This lets `signal.changes`
    * streams emit the updated signal's value when restarting, in such
    * a way that the value propagates to all new observers instead of
    * just the first new observer that triggered restart.
    *
    * For that to happen, you need to wrap the code that's adding several
    * observers into `onStart.shared`. We do this in a couple places in
    * Airstream, and in a couple places in Laminar, and this seems to cover
    * most reasonable use cases. Users might need to wrap some of their code
    * into `onStart.shared` manually if they manage subscriptions manually.
    *
    * See https://github.com/raquo/Airstream/#restarting-streams-that-depend-on-signals--signalchanges-
    */
  object onStart {

    private var level: Int = 0

    private val pendingCallbacks: JsArray[Transaction => Unit] = JsArray()

    /* Put the code that (potentially) adds more than one observer inside.
     * If that code causes `signal.changes` to restart (and emit the signal's
     * updated value), this event will be delayed until the rest of your code
     * in `shared` has finished executing. You can nest `shared` calls if
     * needed, and Airstream will wait for the outermost `shared` block to
     * finish running before executing all pendingCallbacks. Currently this
     * logic is only used to fire those signal.changes events.
     *
     * To be more specific, once the outermost `shared` block finishes executing,
     * a new transaction will be created, and inside of it, all pending callbacks
     * will be executed. Aside from having the benefit of executing after all
     * the desired observers have been added, this also has the benefit of sending
     * out all of those events in the same transaction. This is done to eliminate
     * glitches (Airstream can avoid FRP glitches only inside a single transaction),
     * however this can introduce other glitch-like differences in behaviour
     * compared to the normal flow of events, e.g. observables that could otherwise
     * never possibly emit an event in the same transaction, might do so when
     * they're both triggered by this mechanism at the same time. However, in
     * practice this should hopefully be almost unnoticeable, as the conditions
     * required to trigger this mechanism are rather specific, and the expected
     * type of glitches are less likely to be disruptive than the usual ones.
     *
     * If you rely on standard Laminar features for automatic management of
     * subscriptions, you shouldn't ever need to call this manually.
     *
     * See https://github.com/raquo/Airstream/#restarting-streams-that-depend-on-signals--signalchanges-
     */
    def shared[A](code: => A): A = {
      level += 1
      val result = try {
        code
      } finally {
        level -= 1
        if (level == 0) {
          resolve()
        }
      }
      result
    }

    /** Add a callback to execute once the new shared transaction gets executed.
      *
      * @param callback - Must not throw!
      */
    def add(callback: Transaction => Unit): Unit = {
      pendingCallbacks.push(callback)
    }

    private def resolve(): Unit = {
      if (pendingCallbacks.length > 0) {
        new Transaction(trx => {
          // #TODO[Integrity] What if calling callback(trx) calls onStart.add?
          //  Is it ok to put it into the same list, or should it go into a new list,
          //  to be executed in a separate transaction?
          while (pendingCallbacks.length > 0) {
            val callback = pendingCallbacks.pop()
            try {
              callback(trx)
            } catch {
              case err: Throwable =>
                // #TODO[Integrity] I'm not 100% sure that this is what we need to do here.
                AirstreamError.sendUnhandledError(err)
            }
          }
        })
      }
    }
  }

  private object pendingTransactions {

    /** first transaction is the top of the stack, currently running */
    private val stack: JsArray[Transaction] = JsArray()

    private val children: JsMap[Transaction, JsArray[Transaction]] = new JsMap()

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
        // #TODO[Performance] This pushToStack is taking up 15% of cpu time on a trivial eventbus --> Observer.empty benchmark.
        //  Should we try to optimize it? Since we run the trx immediately, perhaps we could simply set a flag instead of pushing it to the array?
        //  Consider this later when I have moer comprehensive benchmarks.
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
          children.forEach((transactions, _) => numChildren += transactions.length)
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

    def peekStack(): js.UndefOr[Transaction] = {
      // in Javascript, this does not fail, but instead return `undefined` if array is empty.
      stack(0)
    }

    def isClearState: Boolean = stack.length == 0 && children.size == 0

    private def maybeChildrenFor(transaction: Transaction): js.UndefOr[JsArray[Transaction]] = {
      children.get(transaction)
    }

    private def pushToStack(transaction: Transaction): Unit = {
      stack.unshift(transaction)
    }

    private def popStack(): js.UndefOr[Transaction] = {
      // JsArray.shift returns `undefined` if array is empty
      stack.shift()
    }

    private def enqueueChild(parent: Transaction, newChild: Transaction): Unit = {
      //println(s"enqueueChild parent = ${parent.id} newChild = ${newChild.id}")
      val maybeChildren = maybeChildrenFor(parent)
      val noChildrenFound = maybeChildren.isEmpty
      val newChildren = maybeChildren.getOrElse(JsArray())
      newChildren.push(newChild)
      if (noChildrenFound) {
        children.set(parent, newChildren)
      }
    }

    private def dequeueChild(parent: Transaction): js.UndefOr[Transaction] = {
      //println(s"dequeueChild parent = ${parent.id}")
      val maybeParentChildren = maybeChildrenFor(parent)
      maybeParentChildren.filter(_.length > 0).map { parentChildren =>
        val nextChild = parentChildren.shift()
        //println(s"- found some children, first: ${nextChild.id}")
        if (parentChildren.length == 0) {
          children.delete(parent)
          //println("- no children left for this parent, removed parent.")
        } else {
          //println("- removed child, some remaining")
        }
        nextChild
      }
    }
  }

  private[core] def isClearState: Boolean = pendingTransactions.isClearState

  private[airstream] def currentTransaction(): js.UndefOr[Transaction] = pendingTransactions.peekStack()

  private def run(transaction: Transaction): Unit = {
    //println(s"--start trx ${transaction.id}")
    try {
      transaction.code(transaction)
      transaction.resolvePendingObservables()
    } catch {
      case err: Throwable =>
        // #TODO[Integrity] I'm not 100% sure that this is what we want to do here,
        //  but I think it's better than not handling the error at all.
        AirstreamError.sendUnhandledError(err)
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

  // private var lastTransactionId: Int = 0
  //
  // private def nextId(): Int = {
  //   if (lastTransactionId == Int.MaxValue) { // Note: This is lower than JS native Number.MAX_SAFE_INTEGER
  //     lastTransactionId = 1
  //   } else {
  //     lastTransactionId += 1
  //   }
  //   lastTransactionId
  // }
}
