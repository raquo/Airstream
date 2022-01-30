package com.raquo.airstream.core

import com.raquo.airstream.debug.DebuggableObservable
import com.raquo.airstream.flatten.FlattenStrategy
import com.raquo.airstream.flatten.FlattenStrategy.{SwitchSignalStrategy, SwitchSignalStreamStrategy, SwitchStreamStrategy}

// @TODO[Scala3] Put this trait together with BaseObservable in the same file, and make BaseObservable sealed.

/** All the interesting stuff is in [[BaseObservable]].
  * This trait exists only as a sort of type alias for BaseObservable[Observable, A].
  * (I can't use an actual type alias for this due to an illegal cycle)
  */
trait Observable[+A] extends BaseObservable[Observable, A] {}

object Observable {

  implicit val switchStreamStrategy: FlattenStrategy[Observable, EventStream, EventStream] = SwitchStreamStrategy

  implicit val switchSignalStreamStrategy: FlattenStrategy[EventStream, Signal, EventStream] = SwitchSignalStreamStrategy

  implicit val switchSignalStrategy: FlattenStrategy[Signal, Signal, Signal] = SwitchSignalStrategy

  /** Provides debug* methods on Observable: debugSpy, debugLogEvents, debugBreakErrors, etc. */
  implicit def toDebuggableObservable[A](observable: Observable[A]): DebuggableObservable[Observable, A] = new DebuggableObservable[Observable, A](observable)

  // @TODO[Elegance] Maybe use implicit evidence on a method instead?
  implicit class MetaObservable[A, Outer[+_] <: Observable[_], Inner[_]](
    val parent: Outer[Inner[A]]
  ) extends AnyVal {

    @inline def flatten[Output[+_] <: Observable[_]](
      implicit strategy: FlattenStrategy[Outer, Inner, Output]
    ): Output[A] = {
      strategy.flatten(parent)
    }
  }
}
