package com.raquo.airstream.combine

import com.raquo.airstream.common.InternalParentObserver
import com.raquo.airstream.core.AirstreamError.CombinedError
import com.raquo.airstream.core.{SyncObservable, Transaction, WritableObservable}
import com.raquo.ew.JsArray
import org.scalajs.dom

import scala.util.{Failure, Success, Try}

trait CombineObservable[A] extends SyncObservable[A] { this: WritableObservable[A] =>

  /** This should only be called when all inputs are ready.
    * It will throw if the required parent values are missing.
    */
  protected[this] def combinedValue: Try[A]

  /** Parent observers are not immediately active. onStart/onStop regulates that. */
  protected[this] val parentObservers: JsArray[InternalParentObserver[_]] = JsArray()

  // @TODO[Elegance] Not a fan of how inputsReady couples this to its subclasses
  /** Check whether inputs (parent observables' values) are all available to be combined. */
  protected[this] def inputsReady: Boolean

  /** Implementations should call this instead of .fireValue() / .fireTry()
    * Transaction will call `syncFire` when it's time, and that in turn will
    * evaluate maybeCombinedValue and call .fireTry()
    */
  protected[this] def onInputsReady(transaction: Transaction): Unit = {
    if (!transaction.pendingObservables.contains(this)) {
      // println(s"Marking CombineObs($id) as pending in TRX(${transaction.id})")
      transaction.pendingObservables.enqueue(this)
    }
  }

  /** This method is called after this pending observable has been resolved */
  override private[airstream] def syncFire(transaction: Transaction): Unit = {
    // @TODO[Performance] I don't think this inputsReady check is needed here, but not confident enough to remove it.
    if (inputsReady) {
      fireTry(combinedValue, transaction)
    } else {
      // Users, please report this warning to me if you see it.
      dom.console.warn("CombineObservable: inputs not ready when syncFire")
    }
  }

  override protected[this] def onStart(): Unit = {
    parentObservers.forEach(_.addToParent(shouldCallMaybeWillStart = false))
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    parentObservers.forEach(_.removeFromParent())
    super.onStop()
  }

}

object CombineObservable {

  def tupleCombinator[A, B, O](combinator: (A, B) => O)(try1: Try[A], try2: Try[B]): Try[O] = {
    if (try1.isSuccess && try2.isSuccess) {
      Success(combinator(try1.get, try2.get))
    } else {
      Failure(CombinedError(List(try1.toEither.left.toOption, try2.toEither.left.toOption)))
    }
  }

  /** @param combinator MUST NOT THROW! */
  def seqCombinator[A, B](trys: Seq[Try[A]], combinator: Seq[A] => B): Try[B] = {
    // @TODO[Performance] typical combinator will access every value by index,
    //  but `trys` could be a List or something with an O(N) cost for this.
    //  However, I don't think initializing an Array in all cases will be
    //  cheaper, because typical seq sizes are very small here (2-4)
    if (trys.forall(_.isSuccess)) {
      val values = trys.map(_.get)
      Success(combinator(values))
    } else {
      val errors = trys.map {
        case Failure(err) => Some(err)
        case _ => None
      }
      Failure(CombinedError(errors))
    }
  }

}
