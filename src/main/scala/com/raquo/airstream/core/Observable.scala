package com.raquo.airstream.core

import com.raquo.airstream.extensions._
import com.raquo.airstream.flatten.{MergingStrategy, SwitchingStrategy}
import com.raquo.airstream.flatten.FlattenStrategy._
import com.raquo.airstream.split.{SplittableOneObservable, SplittableSeqObservable}
import com.raquo.airstream.status.Status

import scala.util.Try

// @TODO[Scala3] Put this trait together with BaseObservable in the same file, and make BaseObservable sealed.

/** All the interesting stuff is in [[BaseObservable]].
  * This trait exists only as a sort of type alias for BaseObservable[Observable, A].
  * (I can't use an actual type alias for this due to an illegal cycle)
  */
trait Observable[+A]
extends BaseObservable[Observable, A] {}

object Observable
extends ObservableMacroImplicits
with ObservableLowPriorityImplicits {

  /** Provides methods on Observable: split, splitByIndex */
  implicit def toSplittableSeqObservable[Self[+_] <: Observable[_], M[_], Input](observable: BaseObservable[Self, M[Input]]): SplittableSeqObservable[Self, M, Input] = new SplittableSeqObservable(observable)

  /** Provides methods on Observable: splitOne */
  implicit def toSplittableOneObservable[Self[+_] <: Observable[_], Input](observable: BaseObservable[Self, Input]): SplittableOneObservable[Self, Input] = new SplittableOneObservable(observable)

  /** Provides methods on observable: flip, foldBoolean, splitBoolean */
  implicit def toBooleanObservable[Self[+_] <: Observable[_]](observable: BaseObservable[Self, Boolean]): BooleanObservable[Self] = new BooleanObservable(observable)

  /** Provides methods on observable: mapSome, mapFilterSome, foldOption, mapToRight, mapToLeft, splitOption */
  implicit def toOptionObservable[A, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Option[A]]): OptionObservable[A, Self] = new OptionObservable(observable)

  /** Provides methods on observable: mapRight, mapLeft, foldEither, mapToOption, mapLeftToOption, splitEither */
  implicit def toEitherObservable[A, B, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Either[A, B]]): EitherObservable[A, B, Self] = new EitherObservable(observable)

  /** Provides methods on observable: mapSuccess, mapFailure, foldTry, mapToEither, recoverFailure, throwFailure, splitTry */
  implicit def toTryObservable[A, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Try[A]]): TryObservable[A, Self] = new TryObservable(observable)

  /** Provides methods on observable: mapOutput, mapInput, mapResolved, mapPending, foldStatus, splitStatus */
  implicit def toStatusObservable[In, Out, Self[+_] <: Observable[_]](observable: BaseObservable[Self, Status[In, Out]]): StatusObservable[In, Out, Self] = new StatusObservable(observable)

  /** Provides methods on observable: mapOutput, mapInput, mapResolved, mapPending, foldStatus, splitStatus */
  implicit def toSeqObservable[A, Self[+_] <: Observable[_], M[_]](observable: BaseObservable[Self, M[A]]): SeqObservable[A, Self, M] = new SeqObservable(observable)

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
