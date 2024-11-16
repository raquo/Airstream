package com.raquo.airstream.state

import com.raquo.airstream.core.AirstreamError.VarError
import com.raquo.airstream.core.Source.SignalSource
import com.raquo.airstream.core.{AirstreamError, Named, Observer, Signal, Sink, Transaction}
import com.raquo.airstream.extensions.OptionVar
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.split.SplittableVar
import com.raquo.airstream.util.hasDuplicateTupleKeys

import scala.util.{Failure, Success, Try}

/** Var is essentially a Signal that you can write to, so it's a source of state, like EventBus is a source of events.
  *
  * There are two kinds of Vars: [[SourceVar]] and [[DerivedVar]]. The latter you can obtain by calling [[zoom]] on
  * any Var, however, unlike SourceVar, DerivedVar requires an [[Owner]] in order to run.
  */
trait Var[A] extends SignalSource[A] with Sink[A] with Named {

  /** Used to make sure we don't update the same var twice in the same transaction */
  private[state] def underlyingVar: SourceVar[_]

  private[state] def getCurrentValue: Try[A]

  private[state] def setCurrentValue(value: Try[A], transaction: Transaction): Unit

  val signal: StrictSignal[A]

  // --

  val writer: Observer[A] = Observer.fromTry { nextTry =>
    //println(s"> init trx from Var.writer(${nextTry})")
    Transaction(setCurrentValue(nextTry, _))
  }

  /** Write values into a Var of Option[V] without manually wrapping in Some() */
  def someWriter[V](implicit evidence: Option[V] <:< A): Observer[V] = {
    writer.contramapSome
  }

  /** An observer much like writer, but can compose input events with the current value of the var, for example:
    *
    * val v = Var(List(1, 2, 3))
    * val appender = v.updater((acc, nextItem) => acc :+ nextItem)
    * appender.onNext(4) // v now contains List(1, 2, 3, 4)
    *
    * Do not use on failed Vars. Use [[tryUpdater]] on those.
    *
    * @param mod (currValue, nextInput) => nextValue
    */
  def updater[B](mod: (A, B) => A): Observer[B] = Observer.fromTry { nextInputTry =>
    Transaction { trx =>
      nextInputTry match {
        case Success(nextInput) =>
          tryNow() match {
            case Success(currentValue) =>
              val nextValue = Try(mod(currentValue, nextInput)) // this does catch exceptions in mod
              setCurrentValue(nextValue, trx)
            case Failure(err) =>
              AirstreamError.sendUnhandledError(
                VarError("Unable to update a failed Var. Consider Var#tryUpdater instead.", cause = Some(err))
              )
          }
        case Failure(err) =>
          setCurrentValue(Failure[A](err), trx)
      }
    }
  }

  /** @param mod (currValue, nextInput) => nextValue
    *            Note: Must not throw!
    */
  def tryUpdater[B](mod: (Try[A], B) => Try[A]): Observer[B] = Observer.fromTry { nextInputTry =>
    Transaction { trx =>
      nextInputTry match {
        case Success(nextInput) =>
          val nextValue = mod(getCurrentValue, nextInput)
          setCurrentValue(nextValue, trx)
        case Failure(err) =>
          setCurrentValue(Failure[A](err), trx)
      }
    }
  }

  /** Create a strictly evaluated DerivedVar. See also: [[zoomLazy]]. */
  def zoom[B](in: A => B)(out: (A, B) => A)(implicit owner: Owner): Var[B] = {
    new DerivedVar[A, B](this, in, out, owner, displayNameSuffix = ".zoom")
  }

  /** Use this to create a Var that zooms into a field of a class stored
    * in the parent Var, for example:
    *
    * ```scala
    * val fieldVar = formStateVar.zoomLazy(_.field1) {
    *   (formState, newFieldValue) => formState.copy(field1 = newFieldValue)
    * }
    * fieldVar.set(newFieldValue) // updates both fieldVar and formStateVar
    * ```
    *
    * The vars become bidirectionally linked, with the underlying state stored
    * in the parent var, and the derived var providing a read-write lens view
    * into it.
    *
    * The new Var's value will be evaluated only if it has subscribers,
    * or when you get its value with methods like .now(). Its value
    * will not be re-evaluated unnecessarily.
    *
    * Note: if you update a lazy derived Var's value, it is
    * not set directly. Instead, you're updating the parent Var,
    * and it propagates from there (lazily, in case of zoomLazy).
    *
    * Note: `in` and `out` functions should be free of side effects,
    * as they may not get called if the Var's value is not observed.
    */
  def zoomLazy[B](in: A => B)(out: (A, B) => A): Var[B] = {
    val zoomedSignal = new LazyStrictSignal(
      signal, in, displayName, displayNameSuffix = ".zoomLazy.signal"
    )
    new LazyDerivedVar[A, B](
      parent = this,
      signal = zoomedSignal,
      zoomOut = (currValue, nextZoomedValue) => out(currValue, nextZoomedValue),
      displayNameSuffix = ".zoomLazy")
    // #nc Use this new impl in 18.0
    // new LazyDerivedVar2[A, B](
    //   parent = this,
    //   signal = zoomedSignal,
    //   zoomOut = (currValueTry, nextZoomedValueTry) => {
    //     currValueTry.fold(
    //       err => {
    //         // If parent value is error-ed, we can't update with the `out` fn.
    //         AirstreamError.sendUnhandledError(
    //           VarError(s"Unable to zoom out of lazy derived var when the parent var is failed.", cause = Some(err))
    //         )
    //         None
    //       },
    //       currValue =>
    //         // Note: setting the derived var to a failed state writes that failure to the parent var
    //         Some(nextZoomedValueTry.map(out(currValue, _)))
    //     )
    //   },
    //   displayNameSuffix = ".zoomLazy"
    // )
  }

  /** Create a derived Var with an isomorphic transformation, for example
    * by transforming the value in the parent var with a codec.
    *
    * ```scala
    * val personVar = jsonVar.bimap(decodeJsonIntoPerson)(encodePersonToJson)
    * ```
    *
    * The two vars become bidirectionally linked, with the underlying state
    * stored in the parent var, and the derived var providing a transformed
    * read-write view into it.
    *
    * Make sure that round-tripping the values through your `getThis` and
    * `getParent` functions results in an equivalent value, otherwise the
    * derived Var will give you different values than what you may expect.
    *
    * Ideally, `getThis` and `getParent` should not throw, but if they do,
    * both the parent and the derived var will be set into error state with
    * the thrown error.
    */
  def bimap[B](getThis: A => B)(getParent: B => A): Var[B] = {
    val zoomedSignal = new LazyStrictSignal(
      signal, getThis, displayName, displayNameSuffix = ".bimap.signal"
    )
    new LazyDerivedVar2[A, B](
      parent = this,
      signal = zoomedSignal,
      updateParent = (_, nextZoomedValueTry) => {
        Some(nextZoomedValueTry.map(getParent))
      },
      displayNameSuffix = ".bimap"
    )
  }

  /** Distinct events (but keep all errors) by == (equals) comparison */
  def distinct: Var[A] = distinctByFn(_ == _)

  /** Distinct events (but keep all errors) by matching key
    * Note: `key(event)` might be evaluated more than once for each event
    */
  def distinctBy(key: A => Any): Var[A] = distinctByFn(key(_) == key(_))

  /** Distinct events (but keep all errors) by reference equality (eq) */
  def distinctByRef(implicit ev: A <:< AnyRef): Var[A] = distinctByFn(ev(_) eq ev(_))

  /** Distinct events (but keep all errors) using a comparison function */
  def distinctByFn(isSame: (A, A) => Boolean): Var[A] = distinctTry {
    case (Success(prev), Success(next)) => isSame(prev, next)
    case _ => false
  }

  /** Distinct errors only (but keep all events) using a comparison function */
  def distinctErrors(isSame: (Throwable, Throwable) => Boolean): Var[A] = distinctTry {
    case (Failure(prevErr), Failure(nextErr)) => isSame(prevErr, nextErr)
    case _ => false
  }

  /** Distinct all values (both events and errors) using a comparison function */
  def distinctTry(isSame: (Try[A], Try[A]) => Boolean): Var[A] = {
    val distinctSignal = new LazyStrictSignal[A, A](
      signal.distinctTry(isSame), identity, displayName, displayNameSuffix = ".distinct*.signal"
    )
    new LazyDerivedVar2[A, A](
      parent = this,
      signal = distinctSignal,
      updateParent = (currValue, nextValue) =>
        if (isSame(currValue, nextValue)) {
          None
        } else {
          Some(nextValue)
        },
      displayNameSuffix = ".distinct*"
    )
  }

  def setTry(tryValue: Try[A]): Unit = writer.onTry(tryValue)

  final def set(value: A): Unit = setTry(Success(value))

  final def setError(error: Throwable): Unit = setTry(Failure(error))

  /** Do not use on failed Vars. Use [[tryUpdate]] on those.
    *
    * @param mod Note: guarded against exceptions
    */
  def update(mod: A => A): Unit = {
    Transaction { trx =>
      tryNow() match {
        case Success(currentValue) =>
          val nextValue = Try(mod(currentValue)) // this does catch exceptions in mod(currentValue)
          setCurrentValue(nextValue, trx)
        case Failure(err) =>
          AirstreamError.sendUnhandledError(
            VarError("Unable to update a failed Var. Consider Var#tryUpdate instead.", cause = Some(err))
          )
      }
    }
  }

  /** @param mod Note: must not throw */
  def tryUpdate(mod: Try[A] => Try[A]): Unit = {
    //println(s"> init trx from Var.tryUpdate")
    Transaction { trx =>
      val nextValue = mod(getCurrentValue)
      setCurrentValue(nextValue, trx)
    }
  }

  /** Update a boolean Var by flipping its value (true -> false, or false -> true) */
  def invert()(implicit ev: A =:= Boolean): Unit = update(v => ev.flip(!ev(v)))

  /** Observer that writes !var.now(), for vars of booleans. */
  def invertWriter(implicit ev: A =:= Boolean): Observer[Unit] = updater((curr, _) => ev.flip(!ev(curr)))

  @inline def tryNow(): Try[A] = signal.tryNow()

  /** @throws Throwable if currentValue is a Failure */
  @inline def now(): A = signal.now()

  override def toObservable: Signal[A] = signal

  override def toObserver: Observer[A] = writer
}

object Var {

  def apply[A](initial: A): Var[A] = fromTry(Success(initial))

  @inline def fromTry[A](initial: Try[A]): Var[A] = new SourceVar[A](initial)

  @inline def fromEither[A](initial: Either[Throwable, A]): Var[A] = new SourceVar[A](initial.toTry)

  // Unfortunately we need the following tuple types to be concrete classes to satisfy Scala 3

  implicit class VarTuple[A](val tuple: (Var[A], A)) extends AnyVal

  implicit class VarTryTuple[A](val tuple: (Var[A], Try[A])) extends AnyVal

  implicit class VarModTuple[A](val tuple: (Var[A], A => A)) extends AnyVal

  implicit class VarTryModTuple[A](val tuple: (Var[A], Try[A] => Try[A])) extends AnyVal

  /** Set multiple Var values in the same Transaction
    * Example usage: Var.set(var1 -> value1, var2 -> value2)
    *
    * If input contains duplicate vars, reports an Airstream unhandled error.
    * Airstream allows a maximum of one event per observable per transaction.
    */
  def set(values: VarTuple[_]*): Unit = {
    val tryValues: Seq[VarTryTuple[_]] = values.map(t => toTryTuple(t))
    setTry(tryValues: _*)
  }

  /** Set multiple Var values in the same Transaction
    * Example usage: Var.setTry(var1 -> Success(value1), var2 -> Failure(error2))
    *
    * If input contains duplicate vars, reports an Airstream unhandled error.
    * Airstream allows a maximum of one event per observable per transaction.
    */
  def setTry(values: VarTryTuple[_]*): Unit = {
    //println(s"> init trx from Var.set/setTry")
    Transaction { trx =>
      if (hasDuplicateVars(values.map(_.tuple))) {
        throw VarError("Unable to Var.{set,setTry}: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.", cause = None)
      }
      values.foreach(setTryValue(_, trx))
    }
  }

  /** Modify multiple Vars in the same Transaction
    * Example usage: Var.update(var1 -> value1 => value1 + 1, var2 -> value2 => value2 * 2)
    *
    * Mod functions should be PURE.
    * - If a mod throws, the var will be set to a failed state.
    * - If you try to update a failed Var, `Var.update` will post an error to unhandled errors,
    * and none of the Vars will update.
    *
    * Reports an Airstream unhandled error:
    * 1) if currentValue of any of the vars is a Failure.
    * This is atomic: an exception in any of the vars will prevent any of
    * the batched updates in this call from going through.
    * 2) if input contains duplicate vars.
    * Airstream allows a maximum of one event per observable per transaction.
    */
  def update(mods: VarModTuple[_]*): Unit = {
    //println(s"> init trx from Var.update")
    Transaction { trx =>
      if (hasDuplicateVars(mods.map(_.tuple))) {
        throw VarError("Unable to Var.update: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.", cause = None)
      }
      val tryMods: Seq[VarTryModTuple[_]] = mods.map(t => modToTryModTuple(t))
      val vars = mods.map(_.tuple._1)
      try {
        vars.foreach(_.now())
      } catch {
        case err: Throwable =>
          throw VarError("Unable to Var.update a failed Var. Consider Var.tryUpdate instead.", cause = Some(err))
      }
      val tryValues: Seq[VarTryTuple[_]] = tryMods.map(t => tryModToTryTuple(t))
      tryValues.foreach(setTryValue(_, trx))
    }
  }

  /** Modify multiple Vars in the same Transaction
    * Example usage: Var.tryUpdate(var1 -> _.map(_ + 1), var2 -> _.map(_ * 2))
    *
    * Note: provided mods MUST NOT THROW.
    *
    * If input contains duplicate vars, reports an Airstream unhandled error.
    * Airstream allows a maximum of one event per observable per transaction.
    */
  def tryUpdate(mods: VarTryModTuple[_]*): Unit = {
    //println(s"> init trx from Var.tryUpdate")
    Transaction { trx =>
      if (hasDuplicateVars(mods.map(_.tuple))) {
        throw VarError("Unable to Var.tryUpdate: the provided list of vars has duplicates. You can't make an observable emit more than one event per transaction.", cause = None)
      }
      val tryValues: Seq[VarTryTuple[_]] = mods.map(t => tryModToTryTuple(t))
      tryValues.foreach(setTryValue(_, trx))
    }
  }

  @inline private def toTryTuple[A](varTuple: VarTuple[A]): VarTryTuple[A] = {
    VarTryTuple((varTuple.tuple._1, Success(varTuple.tuple._2)))
  }

  @inline private def modToTryModTuple[A](modTuple: VarModTuple[A]): VarTryModTuple[A] = {
    VarTryModTuple((modTuple.tuple._1, (t: Try[A]) => t.map(curr => modTuple.tuple._2(curr))))
  }

  @inline private def tryModToTryTuple[A](modTuple: VarTryModTuple[A]): VarTryTuple[A] = {
    VarTryTuple((modTuple.tuple._1, modTuple.tuple._2(modTuple.tuple._1.tryNow())))
  }

  @inline private def setTryValue[A](tuple: VarTryTuple[A], transaction: Transaction): Unit = {
    tuple.tuple._1.setCurrentValue(tuple.tuple._2, transaction)
  }

  private def hasDuplicateVars(tuples: Seq[(Var[_], _)]): Boolean = {
    hasDuplicateTupleKeys(tuples.map(t => t.copy(_1 = t._1.underlyingVar)))
  }

  /** Provides methods on Var: split, splitMutate */
  implicit def toSplittableVar[M[_], Input](signal: Var[M[Input]]): SplittableVar[M, Input] = new SplittableVar(signal)

  /** Provides methods on Var: splitOption */
  implicit def toOptionVar[A](v: Var[Option[A]]): OptionVar[A] = new OptionVar(v)

}

