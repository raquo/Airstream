package com.raquo.airstream.flatten

import com.raquo.airstream.core.{EventStream, Observable, Signal}

import scala.annotation.implicitNotFound

// format: off

/** Specifies how to flatten observables of observables. */
trait FlattenStrategy[
  -Outer[+_] <: Observable[?],
  -Inner[_],
  +Output[+_] <: Observable[?]
] {
  /** Must not throw */
  def flatten[A](parent: Outer[Inner[A]]): Output[A]
}

/** Flatten strategies with semantics of mirroring the latest emitted observable. */
trait SwitchingStrategy[
  -Outer[+_] <: Observable[?],
  -Inner[_],
  +Output[+_] <: Observable[?]
] extends FlattenStrategy[Outer, Inner, Output]

/** Flatten strategies with semantics of merging all of the emitted observables. */
trait MergingStrategy[
  -Outer[+_] <: Observable[?],
  -Inner[_],
  +Output[+_] <: Observable[?]
] extends FlattenStrategy[Outer, Inner, Output]

// format: on

@implicitNotFound("\nYou are trying to use Airstream's flatMap operator.\nIt was renamed to flatMapSwitch / flatMapMerge to discourage incorrect usage, especially in for-comprehensions.\nSee https://github.com/raquo/Airstream/#avoid-unnecessary-flatmap")
trait AllowFlatMap

@implicitNotFound("\nYou are trying to use Airstream's flatten operator.\nIt was renamed to flattenSwitch / flattenMerge to clarify intent and discourage incorrect usage, similarly to flatMap.\nSee https://github.com/raquo/Airstream/#avoid-unnecessary-flatmap")
trait AllowFlatten

object FlattenStrategy {

  @deprecated("You are using Airstream's deprecated flatMap operator using FlattenStrategy.flatMapAllowed import. This migration helper will be removed in the next version of Airstream. See https://github.com/raquo/Airstream/#avoid-unnecessary-flatmap", since = "17.0.0")
  implicit lazy val allowFlatMap: AllowFlatMap = new AllowFlatMap {}

  @deprecated("You are using Airstream's deprecated flatten operator using FlattenStrategy.flattenAllowed import. This migration helper will be removed in the next version of Airstream. See https://github.com/raquo/Airstream/#avoid-unnecessary-flatmap", since = "17.0.0")
  implicit lazy val allowFlatten: AllowFlatten = new AllowFlatten {}

  /** See docs for [[SwitchStream]] */
  object SwitchStreamStrategy extends SwitchingStrategy[Observable, EventStream, EventStream] {
    override def flatten[A](parent: Observable[EventStream[A]]): EventStream[A] = {
      new SwitchStream[EventStream[A], A](parent = parent, makeStream = identity)
    }
  }

  /** See docs for [[ConcurrentStream]] */
  object ConcurrentStreamStrategy extends MergingStrategy[Observable, EventStream, EventStream] {
    override def flatten[A](parent: Observable[EventStream[A]]): EventStream[A] = {
      new ConcurrentStream[A](parent = parent)
    }
  }

  /** See docs for [[SwitchSignalStream]] */
  object SwitchSignalStreamStrategy extends SwitchingStrategy[EventStream, Signal, EventStream] {
    override def flatten[A](parent: EventStream[Signal[A]]): EventStream[A] = {
      new SwitchSignalStream(parent)
    }
  }

  /** See docs for [[SwitchSignal]] */
  object SwitchSignalStrategy extends SwitchingStrategy[Signal, Signal, Signal] {
    override def flatten[A](parent: Signal[Signal[A]]): Signal[A] = {
      new SwitchSignal(parent)
    }
  }

  object SwitchSignalObservableStrategy extends SwitchingStrategy[Observable, Signal, Observable] {
    override def flatten[A](parent: Observable[Signal[A]]): Observable[A] = {
      parent.matchStreamOrSignal(
        ifStream = SwitchSignalStreamStrategy.flatten,
        ifSignal = SwitchSignalStrategy.flatten
      )
    }
  }

}
