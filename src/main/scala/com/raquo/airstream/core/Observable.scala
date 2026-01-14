package com.raquo.airstream.core

import com.raquo.airstream.debug.DebuggableObservable
import com.raquo.airstream.extensions._
import com.raquo.airstream.flatten.{MergingStrategy, SwitchingStrategy}
import com.raquo.airstream.flatten.FlattenStrategy._
import com.raquo.airstream.split.SplittableObservable
import com.raquo.airstream.status.Status

import scala.util.Try

// @TODO[Scala3] Put this trait together with BaseObservable in the same file, and make BaseObservable sealed.

/** All the interesting stuff is in [[BaseObservable]].
  * This trait exists only as a sort of type alias for BaseObservable[Observable, A].
  * (I can't use an actual type alias for this due to an illegal cycle)
  */
trait Observable[+A] extends BaseObservable[Observable, A] {}

object Observable
extends ObservableMacroImplicits
with ObservableLowPriorityImplicits {

  /** Provides methods on Observable: split, splitByIndex */
  implicit def toSplittableObservavble[M[_], Input](observable: Observable[M[Input]]): SplittableObservable[M, Input] = new SplittableObservable(observable)

  /** Provides debug* methods on Observable: debugSpy, debugLogEvents, debugBreakErrors, etc. */
  implicit def toDebuggableObservable[A](observable: Observable[A]): DebuggableObservable[Observable, A] = new DebuggableObservable[Observable, A](observable)

  /** Provides methods on observable: flip, foldBoolean */
  implicit def toBooleanObservable[Self[+_] <: Observable[_]](observable: BaseObservable[Self, Boolean]): BooleanObservable[Self] = new BooleanObservable(observable)

  /** Provides methods on observable: mapSome, mapFilterSome, foldOption, mapToRight, mapToLeft */
  implicit def toOptionObservable[A, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Option[A]]): OptionObservable[A, Self] = new OptionObservable(observable)

  /** Provides methods on observable: mapRight, mapLeft, foldEither, mapToOption, mapLeftToOption */
  implicit def toEitherObservable[A, B, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Either[A, B]]): EitherObservable[A, B, Self] = new EitherObservable(observable)

  /** Provides methods on observable: mapSuccess, mapFailure, foldTry, mapToEither, recoverFailure, throwFailure */
  implicit def toTryObservable[A, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Try[A]]): TryObservable[A, Self] = new TryObservable(observable)

  /** Provides methods on observable: mapOutput, mapInput, mapResolved, mapPending, foldStatus */
  implicit def toStatusObservable[In, Out, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Status[In, Out]]): StatusObservable[In, Out, Self] = new StatusObservable(observable)

  /** Provides methods on observable: flattenSwitch, flattenMerge, flattenCustom, flatten (deprecated) */
  implicit def toMetaObservable[A, Outer[+_] <: Observable[_], Inner[_]](observable: Outer[Inner[A]]): MetaObservable[A, Outer, Inner] = new MetaObservable(observable)

  implicit val switchStreamStrategy: SwitchingStrategy[Observable, EventStream, EventStream] = SwitchStreamStrategy

  implicit val switchSignalStreamStrategy: SwitchingStrategy[EventStream, Signal, EventStream] = SwitchSignalStreamStrategy

  implicit val switchSignalStrategy: SwitchingStrategy[Signal, Signal, Signal] = SwitchSignalStrategy

  implicit val mergeStreamsStrategy: MergingStrategy[Observable, EventStream, EventStream] = ConcurrentStreamStrategy
}

trait ObservableLowPriorityImplicits {

  implicit val switchSignalObservableStrategy: SwitchingStrategy[Observable, Signal, Observable] = SwitchSignalObservableStrategy
}
