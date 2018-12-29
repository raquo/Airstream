package com.raquo.airstream.signal

import com.raquo.airstream.core.{Observer, Transaction}
import com.raquo.airstream.signal.Var.VarSignal

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

  val writer: Observer[A] = Observer.fromTry {
    case nextTry => new Transaction(setCurrentValue(nextTry, _))
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
    val unsafeValue = now()
    val nextValue = Try(mod(unsafeValue)) // Warning: This line will throw if mod throws, DO NOT move it inside the Transaction
    new Transaction(setCurrentValue(nextValue, _))
  }

  /** @param mod Note: must not throw
    * @throws Exception if `mod` throws
    */
  def tryUpdate(mod: Try[A] => Try[A]): Unit = {
    val nextValue = mod(currentValue) // Warning: This line will throw if mod throws, DO NOT move it inside the Transaction
    new Transaction(setCurrentValue(nextValue, _))
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


  def set(values: VarTuple[_]*): Unit = {
    // @TODO[Performance] Make sure there is no overhead for `_*`
    val tryValues: Seq[VarTryTuple[_]] = values.map(canonizeVarTuple(_))
    setTry(tryValues: _*)
  }

  def setTry(values: VarTryTuple[_]*): Unit = {
    new Transaction(trx => values.foreach(setTryValue(_, trx)))
  }

  /** @throws Exception if currentValue of any of the vars is a Failure.
    *                   This is atomic: an exception in any of the vars will prevent any of
    *                   the batched updates in this call from going through.
    */
  def update(mods: VarModTuple[_]*): Unit = {
    val tryValues: Seq[VarTryTuple[_]] = mods.map(canonizeModTuple(_))
    setTry(tryValues: _*)
  }

  /** Note: none of the provided mods must throw. Same atomic behaviour as `update`.
    * @throws Exception if any of the provided `mod`s throws
    */
  def tryUpdate(mods: VarTryModTuple[_]*): Unit = {
    val tryValues: Seq[VarTryTuple[_]] = mods.map(canonizeTryModTuple(_))
    setTry(tryValues: _*)
  }


  @inline private def canonizeVarTuple[A](varTuple: VarTuple[A]): VarTryTuple[A] = (varTuple._1, Success(varTuple._2))

  @inline private def canonizeModTuple[A](modTuple: VarModTuple[A]): VarTryTuple[A] = (modTuple._1, Success(modTuple._2(modTuple._1.now())))

  @inline private def canonizeTryModTuple[A](modTuple: VarTryModTuple[A]): VarTryTuple[A] = (modTuple._1, modTuple._2(modTuple._1.tryNow()))

  @inline private def setTryValue[A](tuple: VarTryTuple[A], transaction: Transaction): Unit = {
    tuple._1.setCurrentValue(tuple._2, transaction)
  }


  /** Unlike other signals, this signal's current value is always up to date
    * because a subscription is not needed to maintain it.
    *
    * Consequently, we expose its current value with now() / tryNow() methods.
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

