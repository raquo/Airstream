package com.raquo.airstream.signal

import com.raquo.airstream.core.{Observer, Transaction}
import com.raquo.airstream.signal.Var.VarSignal
import com.raquo.airstream.util.hasDuplicateTupleKeys

import scala.util.{Success, Try}

/** Var is a container for a Writeable Signal – sort of like EventBus, but for Signals.
  *
  * Note that while this Var and its signal itself are strict – that is, their currentValue will update regardless
  * of whether the Var's `signal` has any observers, this new value will not propagate anywhere if the signal has
  * no observers, obviously.
  */
class Var[A] private(private[this] var currentValue: Try[A]) {

  /** VarSignal is a private type, do not expose it */
  private[this] val _varSignal = new VarSignal[A](initialValue = currentValue)

  val signal: StrictSignal[A] = _varSignal

  val writer: Observer[A] = Observer.fromTry { case nextTry => // Note: `case` syntax needed for Scala 2.12
    //println(s"> init trx from Var.writer(${nextTry})")
    new Transaction(setCurrentValue(nextTry, _))
  }

  @inline def set(value: A): Unit = writer.onNext(value)

  @inline def setTry(tryValue: Try[A]): Unit = writer.onTry(tryValue)

  @inline def setError(error: Throwable): Unit = writer.onError(error)

  /** @param mod Note: guarded against exceptions
    * @throws Exception if currentValue is a Failure */
  def update(mod: A => A): Unit = {
    //    setCurrentValue(currentValue.fold[Try[A]](
    //      err => throw VarUpdateError(cause = err),
    //      value => Try(mod(value))
    //    ))
    //println(s"> init trx from Var.update")
    new Transaction(trx => {
      val unsafeValue = now()
      val nextValue = Try(mod(unsafeValue)) // Note: this does catch exceptions in mod(unsafeValue)
      setCurrentValue(nextValue, trx)
    })
  }

  /** @param mod Note: must not throw
    * @throws Exception if `mod` throws
    */
  def tryUpdate(mod: Try[A] => Try[A]): Unit = {
    //println(s"> init trx from Var.tryUpdate")
    new Transaction(trx => {
      val nextValue = mod(currentValue) // Note: this does catch exceptions in mod(unsafeValue)
      setCurrentValue(nextValue, trx)
    })
  }

  @inline def tryNow(): Try[A] = signal.tryNow()

  /** @throws Exception if currentValue is a Failure */
  @inline def now(): A = signal.now()

  private[Var] def setCurrentValue(value: Try[A], transaction: Transaction): Unit = {
    currentValue = value
    _varSignal.onTry(value, transaction)
  }
}

object Var {

  def apply[A](initial: A): Var[A] = fromTry(Success(initial))

  @inline def fromTry[A](initial: Try[A]): Var[A] = new Var(initial)


  type VarTuple[A] = (Var[A], A)

  type VarTryTuple[A] = (Var[A], Try[A])

  type VarModTuple[A] = (Var[A], A => A)

  type VarTryModTuple[A] = (Var[A], Try[A] => Try[A])

  /** Set multiple Var values in the same Transaction
    * Example usage: Var.set(var1 -> value1, var2 -> value2)
    */
  def set(values: VarTuple[_]*): Unit = {
    val tryValues: Seq[VarTryTuple[_]] = values.map(toTryTuple(_))
    setTry(tryValues: _*)
  }

  /** Set multiple Var values in the same Transaction
    * Example usage: Var.setTry(var1 -> Success(value1), var2 -> Failure(error2))
    */
  def setTry(values: VarTryTuple[_]*): Unit = {
    //println(s"> init trx from Var.set/setTry")
    if (hasDuplicateTupleKeys(values)) {
      throw new Exception("Unable to Var.{set,setTry}: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.")
    }
    new Transaction(trx => values.foreach(setTryValue(_, trx)))
  }

  /** Modify multiple Vars in the same Transaction
    * Example usage: Var.update(var1 -> value1 => value1 + 1, var2 -> value2 => value2 * 2)
    *
    * Mod functions should be PURE and MUST NOT THROW.
    * If you try to update a failed Var, or if some of the mods throw,
    * `Var.update` will throw and none of the Vars will update.
    *
    * @throws Exception if currentValue of any of the vars is a Failure.
    *                   This is atomic: an exception in any of the vars will prevent any of
    *                   the batched updates in this call from going through.
    */
  def update(mods: VarModTuple[_]*): Unit = {
    if (hasDuplicateTupleKeys(mods)) {
      throw new Exception("Unable to Var.update: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.")
    }
    val tryMods: Seq[VarTryModTuple[_]] = mods.map(modToTryModTuple(_))
    //println(s"> init trx from Var.update")
    new Transaction(trx => {
      val tryValues: Seq[VarTryTuple[_]] = tryMods.map(tryModToTryTuple(_))
      if (tryValues.exists(_._2.isFailure)) {
        throw new Exception(s"Unable to Var.update because one of the ${mods.length} Var(s) would have failed, or was failed from the start. Use Var.tryUpdate if this is really what you want.")
      }
      tryValues.foreach(setTryValue(_, trx))
    })
  }

  /** Modify multiple Vars in the same Transaction
    * Example usage: Var.tryUpdate(var1 -> _.map(_ + 1), var2 -> _.map(_ * 2))
    *
    * Note: none of the provided mods must throw. Same atomic behaviour as `update`.
    * @throws Exception if any of the provided `mod`s throws
    */
  def tryUpdate(mods: VarTryModTuple[_]*): Unit = {
    //println(s"> init trx from Var.tryUpdate")
    if (hasDuplicateTupleKeys(mods)) {
      throw new Exception("Unable to Var.tryUpdate: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.")
    }
    new Transaction(trx => {
      val tryValues: Seq[VarTryTuple[_]] = mods.map(tryModToTryTuple(_))
      tryValues.foreach(setTryValue(_, trx))
    })
  }

  @inline private def toTryTuple[A](varTuple: VarTuple[A]): VarTryTuple[A] = (varTuple._1, Success(varTuple._2))

  @inline private def modToTryModTuple[A](modTuple: VarModTuple[A]): VarTryModTuple[A] = (modTuple._1, _.map(curr => modTuple._2(curr)))

  @inline private def tryModToTryTuple[A](modTuple: VarTryModTuple[A]): VarTryTuple[A] = (modTuple._1, modTuple._2(modTuple._1.tryNow()))

  @inline private def setTryValue[A](tuple: VarTryTuple[A], transaction: Transaction): Unit = {
    tuple._1.setCurrentValue(tuple._2, transaction)
  }


  /** Unlike other signals, this signal's current value is always up to date
    * because a subscription is not needed to maintain it.
    *
    * Consequently, we expose its current value with now() / tryNow() methods
    * (see StrictSignal).
    */
  class VarSignal[A] private[Var](
    override protected[this] val initialValue: Try[A]
  ) extends StrictSignal[A] {

    /** Var does not directly depend on other streams, so it breaks the graph. */
    override protected[airstream] val topoRank: Int = 1

    /** Note: we do not check if isStarted() here, this is how we ensure that this
      * signal's current value stays up to date. If this signal is stopped, this
      * value will not be propagated anywhere further though.
      */
    private[Var] def onTry(nextValue: Try[A], transaction: Transaction): Unit = {
      fireTry(nextValue, transaction)
    }
  }

}

