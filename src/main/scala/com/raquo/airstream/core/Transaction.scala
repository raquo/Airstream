package com.raquo.airstream.core

import com.raquo.airstream.combine.MergeStream
import com.raquo.airstream.core.AirstreamError.TransactionDepthExceeded
import com.raquo.airstream.custom.CustomSource
import com.raquo.airstream.util.JsPriorityQueue
import com.raquo.ew.{JsArray, JsMap}

import scala.annotation.tailrec
import scala.scalajs.js

/** Transaction is a moment in time during which Airstream guarantees no FRP glitches.
  *
  * Some observables need to emit their events in new transactions. Roughly speaking:
  * - All async observables (e.g. delay(100))
  * - All observables that can create loops in the observable graph (e.g. flatMapSwitch)
  * - All observables that get their events from outside of Airstream (e.g. custom sources)
  *
  * An observable can only emit once in a given transaction.
  * - See [[MergeStream]] or [[CustomSource]] for examples of handling that.
  *
  * See the docs for more details.
  *
  * @param code Note: Must not throw!
  */
class Transaction(private[Transaction] var code: Transaction => Any) {

  // val id = Transaction.nextId()

  //println(s"--CREATE Trx@${id}")

  //override def toString: String = s"Trx@${id}"

  /** Priority queue of pending observables: sorted by their topoRank.
    *
    * Corollary: An Observable that is dequeue-d from here does not synchronously depend on any other pending observables
    */
  private[this] var maybePendingObservables: js.UndefOr[JsPriorityQueue[SyncObservable[_]]] = js.undefined

  /**
    * Note: The transaction may be _actually scheduled_ one layer deeper
    * than this `depth` due to the `postStartTransactions` mechanism,
    * but this is good enough for `maxDepth` purposes.
    */
  private val depth: Int = Transaction.pendingTransactions.peekStack().fold(1)(_.depth + 1)

  if (Transaction.maxDepth == -1 || depth > Transaction.maxDepth) {
    // Short circuit to break out of infinite loops without pinning CPU or crashing the whole app.
    // See e.g. https://github.com/raquo/Laminar/issues/116
    // This transaction will not be executed. Instead, it is reported in unhandled errors.
    // Other transactions will continue executing, if you have any that don't exceed the depth.
    // We don't throw an exception here because I'm afraid this could break things too violently,
    // and it's not like there is a reasonable way to locally handle such a condition anyway.
    AirstreamError.sendUnhandledError(TransactionDepthExceeded(this, Transaction.maxDepth))
  } else {
    if (Transaction.onStart.isSharedStart) {
      // This delays scheduling transactions until the end of
      // the shared start transaction
      //println(s">>> onStart.postStartTransactions.push($this)")
      Transaction.onStart.postStartTransactions.push(this)
    } else {
      //println(s">>> Transaction.pendingTransactions.add($this)")
      Transaction.pendingTransactions.add(this)
    }
  }

  @inline private[Transaction] def resolvePendingObservables(): Unit = {
    //println(s"$this resolvePendingObservables (n=${maybePendingObservables.map(_.size).getOrElse(0)})")
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

  /** Create new transaction.
    *
    * Typically used in internal observable code.
    *
    * Warning: It is rare that end users need to manually create transactions.
    * Example of legitimate use case: [[https://github.com/raquo/Airstream/#var-transaction-delay Var transaction delay]]
    */
  def apply(code: Transaction => Unit): Unit = new Transaction(code)

  /** How many _nested_ transactions you can have.
    *
    * Airstream will refuse to schedule any transaction that exceeds this limit.
    * This will prevent the content of the transaction from running, so this
    * could leave an event not fully propagated. This is, of course, an
    * undesirable situation, so you should never hit this limit.
    *
    * It is very hard to deliberately write valid code that would hit this
    * limit. If you are hitting this limit, most likely:
    * - There is an (unterminated) infinite loop in your observable graph,
    *   e.g. two Var-s updating each other recursively, or
    * - You are doing something that looks a lot like that for the first
    *   `maxDepth` transactions.
    *
    * You probably need to adjust your code instead of raising this limit,
    * but if you have a valid need for raising it, please let me know.
    *
    * You can set this to -1 to disable the check completely, but then
    * if you do actually have an infinite loop, your code will pin the
    * CPU and your single threaded Scala.js app will freeze – not
    * something you want, especially on your users' devices!
    */
  var maxDepth: Int = 1000

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

    private[Transaction] var isSharedStart: Boolean = false

    private val pendingCallbacks: JsArray[Transaction => Unit] = JsArray()

    private[Transaction] val postStartTransactions: JsArray[Transaction] = JsArray()

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
    def shared[A](code: => A, when: Boolean = true): A = {
      if (isSharedStart || !when) {
        // - We are already executing inside the `code` argument passed
        //   to another onStart.shared block, so adding another try-catch
        //   block is not necessary: that other block will take care of it.
        // - Or, the caller explicitly doesn't want a shared block now (!when)
        code
      } else {
        //println("> START SHARED")
        isSharedStart = true
        val result = try {
          code
        } finally {
          isSharedStart = false
          resolve()
        }
        //println("< END SHARED")
        result
      }
    }

    /** Add a callback to execute once the new shared transaction gets executed.
      *
      * @param callback - Must not throw!
      */
    def add(callback: Transaction => Unit): Unit = {
      //println(s"// add callback ${callback.hashCode()}")
      pendingCallbacks.push(callback)
    }

    private def resolve(): Unit = {
      if (pendingCallbacks.length == 0) {
        //println("- no pending callbacks")
        if (postStartTransactions.length > 0) {
          //println(s"> CREATE ALT RESOLVE TRX. Num trx-s = ${postStartTransactions.length}")
          Transaction { _ =>
            while (postStartTransactions.length > 0) {
              pendingTransactions.add(postStartTransactions.shift())
            }
          }
        }
      } else {
        //println(s"> CREATE RESOLVE TRX. Num callbacks = ${pendingCallbacks.length}")
        Transaction { trx =>
          // #TODO[Integrity] What if calling callback(trx) calls onStart.add?
          //  Is it ok to put it into the same list, or should it go into a new list,
          //  to be executed in a separate transaction?
          while (pendingCallbacks.length > 0) {
            val callback = pendingCallbacks.shift()
            // println(s"// resolve callback ${callback.hashCode()}")
            try {
              callback(trx)
            } catch {
              case err: Throwable =>
                // #TODO[Integrity] I'm not 100% sure that this is what we need to do here.
                AirstreamError.sendUnhandledError(err)
            }
          }
          // println("// resolved any callbacks")
          // Any transactions created during the shared start
          // that weren't converted to callbacks, will now be
          // scheduled, and will be executed after this shared
          // transaction finishes (they are marked as its
          // children), in the same order as they were created.
          // println(s"postStartTransactions.length = ${postStartTransactions.length}")
          while (postStartTransactions.length > 0) {
            val _trx = postStartTransactions.shift()
            // println(s"- pendingTransactions.add(${t})")
            pendingTransactions.add(_trx)
          }
        }
      }
    }
  }

  private object pendingTransactions {

    /** First transaction is the top of the stack, currently running.
      * That transaction's parent transaction is the second item, and so on.
      */
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
        done(newTransaction)
      } { currentTransaction =>
        enqueueChild(parent = currentTransaction, newChild = newTransaction)
      }
    }

    /** #Warning: you MUST call done(trx) after calling `run(trx)`! */
    @tailrec def done(transaction: Transaction): Unit = {
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

      transaction.code = throwDeadTrxError // stop holding up `trx` contents in memory

      val maybeNextTransaction = peekStack()
      if (maybeNextTransaction == js.undefined) {
        if (children.size > 0) {
          // dom.console.log(s"Stack is empty but children remain: ${children.map(t => (t._1.id, t._2.map(_.id)))}")
          var numChildren = 0
          children.forEach((transactions, _) => numChildren += transactions.length)
          throw new Exception(s"Transaction queue error: Stack cleared, but a total of ${numChildren} children for ${children.size} transactions remain. This is a bug in Airstream.")
        }
      } else {
        val nextTransaction = maybeNextTransaction.asInstanceOf[Transaction]
        run(nextTransaction)
        done(nextTransaction)
      }
    }

    /* If this transaction has children remaining, set first child to be run next.
     * Otherwise, remove transaction from the stack and do the same for next transaction on stack.
     */
    @tailrec def putNextTransactionOnStack(doneTransaction: Transaction): Unit = {
      // We use depth-first because of https://github.com/raquo/Airstream/issues/39
      val maybeNextChildTrx = dequeueChild(parent = doneTransaction)
      if (maybeNextChildTrx == js.undefined) {
        // No children, this transaction is truly done now, remove it from the stack.
        popStack()
        // If any transactions left in the stack, recurse
        val maybeParentTransaction = peekStack()
        if (maybeParentTransaction != js.undefined) {
          val parentTransaction = maybeParentTransaction.asInstanceOf[Transaction]
          putNextTransactionOnStack(doneTransaction = parentTransaction)
        }
      } else {
        val nextChildTrx = maybeNextChildTrx.asInstanceOf[Transaction]
        // Found a child transaction, so put it on the stack, so that it wil run next.
        // Once that child is all done, it will be popped from the stack, and we will
        pushToStack(nextChildTrx)
      }
    }

    /** Returns the top of the stack, i.e. the currently executing transaction, if any. */
    def peekStack(): js.UndefOr[Transaction] = {
      // in Javascript, if array is empty, this does not fail, but instead returns `undefined`.
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

  //private var maybeCurrentTransaction: js.UndefOr[Transaction] = js.undefined

  private[airstream] def currentTransaction(): js.UndefOr[Transaction] = pendingTransactions.peekStack()

  /** #Warning: you MUST call `done(trx)` after calling `run(trx)`! */
  private def run(transaction: Transaction): Unit = {
    //println(s"--START ${transaction}")
    //maybeCurrentTransaction = transaction
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
      //  but it doesn't actually catch the exception, so `Transaction(code)` actually throws
      //  iff `code` throws AND the transaction was created while no other transaction is running
      //  This is not very predictable, so we should fix it.
      //println(s"--END ${transaction}")
      //maybeCurrentTransaction = js.undefined
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
