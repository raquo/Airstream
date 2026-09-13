package com.raquo.airstream.core

import com.raquo.airstream.extensions._
import com.raquo.airstream.flatten.{MergingStrategy, SwitchingStrategy}
import com.raquo.airstream.flatten.FlattenStrategy._
import com.raquo.airstream.split.{SplittableOneObservable, SplittableSeqObservable, SplittableSeqOptionObservable}
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

  /** Provides methods on Observable: splitSeq, splitSeqByIndex */
  implicit def toSplittableSeqObservable[Self[+_] <: Observable[?], M[_], Input](observable: BaseObservable[Self, M[Input]]): SplittableSeqObservable[Self, M, Input] = new SplittableSeqObservable(observable)

  /** Provides methods on Observable: splitSomeSeq, splitSomeSeqByIndex */
  implicit def toSplittableSeqOptionObservable[Self[+_] <: Observable[?], M[_], Input](observable: BaseObservable[Self, Option[M[Input]]]): SplittableSeqOptionObservable[Self, M, Input] = new SplittableSeqOptionObservable(observable)

  /** Provides methods on Observable: splitOne */
  implicit def toSplittableOneObservable[Self[+_] <: Observable[?], Input](observable: BaseObservable[Self, Input]): SplittableOneObservable[Self, Input] = new SplittableOneObservable(observable)

  /** Provides methods on observable: flip, foldBoolean, splitBoolean */
  implicit def toBooleanObservable[Self[+_] <: Observable[?]](observable: BaseObservable[Self, Boolean]): BooleanObservable[Self] = new BooleanObservable(observable)

  /** Provides methods on observable: mapSome, mapFilterSome, foldOption, mapToRight, mapToLeft, splitOption */
  implicit def toOptionObservable[A, Self[+_] <: Observable[?]](observable: BaseObservable[Self, Option[A]]): OptionObservable[A, Self] = new OptionObservable(observable)

  /** Provides methods on observable: mapRight, mapLeft, foldEither, mapToOption, mapLeftToOption, splitEither */
  implicit def toEitherObservable[A, B, Self[+_] <: Observable[?]](observable: BaseObservable[Self, Either[A, B]]): EitherObservable[A, B, Self] = new EitherObservable(observable)

  /** Provides methods on observable: mapSuccess, mapFailure, foldTry, mapToEither, recoverFailure, throwFailure, splitTry */
  implicit def toTryObservable[A, Self[+_] <: Observable[?]](observable: BaseObservable[Self, Try[A]]): TryObservable[A, Self] = new TryObservable(observable)

  /** Provides methods on observable: mapOutput, mapInput, mapResolved, mapPending, foldStatus, splitStatus */
  implicit def toStatusObservable[In, Out, Self[+_] <: Observable[?]](observable: BaseObservable[Self, Status[In, Out]]): StatusObservable[In, Out, Self] = new StatusObservable(observable)

  /** Provides methods on observable: mapSeq, seqOrElse */
  implicit def toSeqObservable[A, Self[+_] <: Observable[?], M[_]](observable: BaseObservable[Self, M[A]]): SeqObservable[A, Self, M] = new SeqObservable(observable)

  /** Provides methods on observable: mapSeqOpt, seqOptOrElse */
  implicit def toSeqOptionObservable[A, Self[+_] <: Observable[?], M[_]](observable: BaseObservable[Self, Option[M[A]]]): SeqOptionObservable[A, Self, M] = new SeqOptionObservable(observable)

  /** Provides methods on observable: flattenSwitch, flattenMerge, flattenCustom, flatten (deprecated) */
  implicit def toMetaObservable[A, Outer[+_] <: Observable[?], Inner[_]](observable: Outer[Inner[A]]): MetaObservable[A, Outer, Inner] = new MetaObservable(observable)

  implicit val switchStreamStrategy: SwitchingStrategy[Observable, EventStream, EventStream] = SwitchStreamStrategy

  implicit val switchSignalStreamStrategy: SwitchingStrategy[EventStream, Signal, EventStream] = SwitchSignalStreamStrategy

  implicit val switchSignalStrategy: SwitchingStrategy[Signal, Signal, Signal] = SwitchSignalStrategy

  implicit val mergeStreamsStrategy: MergingStrategy[Observable, EventStream, EventStream] = ConcurrentStreamStrategy
}

trait ObservableLowPriorityImplicits {

  implicit val switchSignalObservableStrategy: SwitchingStrategy[Observable, Signal, Observable] = SwitchSignalObservableStrategy

  // The methods below provide `mapSomes` and `splitOptions` methods on observables of tuples of options
  // #TODO[Org]: These don't have to be lower priority, they're just here for more ergonomic organization

  implicit def toOptionTupleObservable2[T1, T2, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2])]): OptionTupleObservable2[T1, T2, Self] = new OptionTupleObservable2(observable)

  implicit def toOptionTupleObservable3[T1, T2, T3, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3])]): OptionTupleObservable3[T1, T2, T3, Self] = new OptionTupleObservable3(observable)

  implicit def toOptionTupleObservable4[T1, T2, T3, T4, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4])]): OptionTupleObservable4[T1, T2, T3, T4, Self] = new OptionTupleObservable4(observable)

  implicit def toOptionTupleObservable5[T1, T2, T3, T4, T5, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5])]): OptionTupleObservable5[T1, T2, T3, T4, T5, Self] = new OptionTupleObservable5(observable)

  implicit def toOptionTupleObservable6[T1, T2, T3, T4, T5, T6, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6])]): OptionTupleObservable6[T1, T2, T3, T4, T5, T6, Self] = new OptionTupleObservable6(observable)

  implicit def toOptionTupleObservable7[T1, T2, T3, T4, T5, T6, T7, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7])]): OptionTupleObservable7[T1, T2, T3, T4, T5, T6, T7, Self] = new OptionTupleObservable7(observable)

  implicit def toOptionTupleObservable8[T1, T2, T3, T4, T5, T6, T7, T8, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8])]): OptionTupleObservable8[T1, T2, T3, T4, T5, T6, T7, T8, Self] = new OptionTupleObservable8(observable)

  implicit def toOptionTupleObservable9[T1, T2, T3, T4, T5, T6, T7, T8, T9, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9])]): OptionTupleObservable9[T1, T2, T3, T4, T5, T6, T7, T8, T9, Self] = new OptionTupleObservable9(observable)

  implicit def toOptionTupleObservable10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10])]): OptionTupleObservable10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Self] = new OptionTupleObservable10(observable)

  implicit def toOptionTupleObservable11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11])]): OptionTupleObservable11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Self] = new OptionTupleObservable11(observable)

  implicit def toOptionTupleObservable12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12])]): OptionTupleObservable12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Self] = new OptionTupleObservable12(observable)

  implicit def toOptionTupleObservable13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13])]): OptionTupleObservable13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Self] = new OptionTupleObservable13(observable)

  implicit def toOptionTupleObservable14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14])]): OptionTupleObservable14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Self] = new OptionTupleObservable14(observable)

  implicit def toOptionTupleObservable15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15])]): OptionTupleObservable15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Self] = new OptionTupleObservable15(observable)

  implicit def toOptionTupleObservable16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15], Option[T16])]): OptionTupleObservable16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Self] = new OptionTupleObservable16(observable)

  implicit def toOptionTupleObservable17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15], Option[T16], Option[T17])]): OptionTupleObservable17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Self] = new OptionTupleObservable17(observable)

  implicit def toOptionTupleObservable18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15], Option[T16], Option[T17], Option[T18])]): OptionTupleObservable18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Self] = new OptionTupleObservable18(observable)

  implicit def toOptionTupleObservable19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15], Option[T16], Option[T17], Option[T18], Option[T19])]): OptionTupleObservable19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Self] = new OptionTupleObservable19(observable)

  implicit def toOptionTupleObservable20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15], Option[T16], Option[T17], Option[T18], Option[T19], Option[T20])]): OptionTupleObservable20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Self] = new OptionTupleObservable20(observable)

  implicit def toOptionTupleObservable21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15], Option[T16], Option[T17], Option[T18], Option[T19], Option[T20], Option[T21])]): OptionTupleObservable21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, Self] = new OptionTupleObservable21(observable)

  implicit def toOptionTupleObservable22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, Self[+_] <: Observable[?]](observable: BaseObservable[Self, (Option[T1], Option[T2], Option[T3], Option[T4], Option[T5], Option[T6], Option[T7], Option[T8], Option[T9], Option[T10], Option[T11], Option[T12], Option[T13], Option[T14], Option[T15], Option[T16], Option[T17], Option[T18], Option[T19], Option[T20], Option[T21], Option[T22])]): OptionTupleObservable22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, Self] = new OptionTupleObservable22(observable)
}
